The trust cert ca.p12 is copied from eric-bos-dr-stub repository.

The client cert 'client.p12' is generated and signed by the self-signed root CA located in the eric-bos-dr-stub repository.

#Create client private key
openssl genrsa -des3 -out client.key 2048 # pass-phrase='password'

# Create Certificate signing request for client
openssl req -key client.key -new -out client.csr
-- The Common Name should correspond to the host of the server on which the cert is being installed, 'localhost'.

# Create client cert, signed by root CA in eric-bos-dr-stub repository
openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in client.csr -out client.crt -days 365 -CAcreateserial

# Creating PKCS12 client keystore
cat client.key client.crt > client.pem
openssl pkcs12 -export -in client.pem -out client.p12

# Create truststore JKS
keytool -importkeystore -srckeystore ca.p12 -srcstoretype PKCS12 -destkeystore truststore.jks  -deststoretype JKS -srcstorepass password -deststorepass password

# Create keystore JKS
keytool -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 -destkeystore keystore.jks  -deststoretype JKS -srcstorepass password -deststorepass password