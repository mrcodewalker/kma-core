# Hướng dẫn cài đặt và cấu hình hệ thống hỗ trợ học tập thông minh

### Bước 1: Truy cập vào github và clone dự án về máy hiện tại

`$ git clone https://github.com/mrcodewalker/kma-core.git
$ cd kma-core/KMALegend`

### Bước 2: Tự tạo 1 file SYSTEM32.env mục đích file này để chạy các biến môi trường được định nghĩa ở đây

```env
SPRING_DATASOURCE_URL=jdbc:mysql://36.50.54.109:3306/kmalegend
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
SERVER_PORT=8765
API_PREFIX=/api/v1
SPRINGDOC_ENABLED=true
SPRING_JPA_SHOW_SQL=true
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect
SPRING_JPA_HIBERNATE_FORMAT_SQL=true
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_MVC_CORS_ALLOWED_ORIGINS=https://kma-legend.click
JWT_SECRET=Z54uiPhveohL/uORp8a8rHhu0qalR4Mj+aIOz5ZA5zY=
JWT_EXPIRATION=86400000
ENABLED_SWAGGER=true
```

### Bước 3: Ở thư mục config trong dự án hãy sửa đổi file WebConfig.java để CORS tương ứng với domain mới của bạn hoặc giữ nguyên nếu muốn chạy local
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins("https://kma-legend.click", "http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
}
```

### Bước 4: Chạy câu lệnh đóng gói chương trình và triển khai lên server
`$ mvn clean
$ mvn install`

### Bước 5: Kiểm tra file đóng gói .jar ở trong thư mục target ( cùng cấp với src )
 <img width="610" height="583" alt="image" src="https://github.com/user-attachments/assets/bd56a512-ae90-439b-8b4d-d4b1290a4679" />

### Bước 6: Thực hiện copy file KMALegend-0.0.1-SNAPSHOT.jar sang server Linux
 <img width="945" height="355" alt="image" src="https://github.com/user-attachments/assets/894cbc7c-0e3f-4ec8-b6ec-1ea3ddb7b7c3" />

### Bước 7: Tạo ra file restart.bash để có thể tiện dễ dàng khởi động lại chương trình nếu có thay đổi chương trình mới
`$ kill -9 $(pgrep -f KMALegend)
$ nohup java -jar KMALegend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
$ sleep 2
$ tail -f app.log`

###   Bước 8: Mở firewall port backend đang chạy
`$ sudo ufw allow 8765
$ sudo ufw reload
$ sudo ufw status`

### Bước 9: Cấu hình nginx cho backend server
```nginx
# configuration file /etc/nginx/sites-enabled/kma-legend:
server {
    server_name kma-legend.click www.kma-legend.click;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Spring Boot API
    location /api/ {
        proxy_pass http://127.0.0.1:8765;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Angular Frontend
    location / {
        root /var/www/kma_angular/dist/kma_angular;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Static files với cache
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /var/www/kma_angular/dist/kma_angular;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    listen 443 ssl http2; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/kma-legend.click/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/kma-legend.click/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name kma-legend.click www.kma-legend.click;
    return 301 https://$host$request_uri;
}
```

### Bước 10: Cấu hình nginx thành công và kiểm tra
`sudo nginx -t
sudo systemctl reload nginx`
