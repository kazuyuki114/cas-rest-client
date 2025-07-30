#!/bin/bash

echo "=== CAS Client SSL Setup ==="

# Check if server.crt exists in current directory
if [ -f "server.crt" ]; then
    echo "Found server.crt in current directory"
    cp server.crt src/main/resources/
    echo "Certificate copied to src/main/resources/"
elif [ -f "src/main/resources/server.crt" ]; then
    echo "server.crt already exists in resources directory"
else
    echo "ERROR: server.crt not found!"
    echo "Please place your server.crt file in the project root directory"
    echo "or directly in src/main/resources/server.crt"
    exit 1
fi

echo "=== SSL Setup Complete ==="
echo "You can now test the SSL connection with:"
echo "curl http://localhost:8081/api/auth/test-ssl" 