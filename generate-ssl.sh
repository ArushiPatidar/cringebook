#!/bin/bash

# Script to generate self-signed SSL certificate for CringeBook

echo "Generating self-signed SSL certificate for CringeBook..."

# Create resources directory if it doesn't exist
mkdir -p src/main/resources

# Generate keystore with self-signed certificate
keytool -genkeypair \
    -alias cringebook \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore src/main/resources/keystore.p12 \
    -validity 365 \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=localhost,OU=CringeBook,O=CringeBook,L=City,S=State,C=US" \
    -ext SAN=dns:localhost,ip:127.0.0.1,dns:cringebook-by-arushi.centralindia.cloudapp.azure.com

if [ $? -eq 0 ]; then
    echo "SSL certificate generated successfully!"
    echo "Certificate location: src/main/resources/keystore.p12"
    echo "Certificate password: changeit"
    echo "Certificate alias: cringebook"
    
    # Update application.properties to enable HTTPS
    echo ""
    echo "Updating application.properties for HTTPS..."
    
    # Backup original file
    cp src/main/resources/application.properties src/main/resources/application.properties.backup
    
    # Update the configuration
    cat > src/main/resources/application.properties << EOF
spring.application.name=app
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mysql://localhost:3306/data
spring.datasource.username=root
spring.datasource.password=Admin@123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.show-sql: true
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# HTTPS Configuration
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=cringebook

# Redirect HTTP to HTTPS
server.require-ssl=false
# Additional HTTP port for redirect (if needed)
server.http.port=8080
EOF
    
    echo "HTTPS configuration updated successfully!"
    echo ""
    echo "To run the application with HTTPS:"
    echo "1. Run: chmod +x generate-ssl.sh"
    echo "2. Run: ./generate-ssl.sh (if not already done)"
    echo "3. Run: mvn spring-boot:run"
    echo "4. Access: https://localhost:8443/frontend/login.html"
    echo ""
    echo "Note: Browser will show security warning for self-signed certificate."
    echo "Click 'Advanced' -> 'Proceed to localhost (unsafe)' to continue."
    
else
    echo "Failed to generate SSL certificate!"
    echo "Make sure keytool is installed (part of JDK)"
    exit 1
fi