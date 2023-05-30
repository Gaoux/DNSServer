# DNS Server

This is a simple implementation of a DNS server in Java.

## Prerequisites

- Java Development Kit (JDK) installed
- Text file `masterFile.txt` containing domain-name to IP-address mappings

## Configuration

The DNS server can be configured with the following constants in the `DNSServer` class:

- `DNS_PORT`: The port number on which the server listens for DNS requests (default: `53`).
- `FOREIGN_RESOLVER_IP`: The IP address of a foreign resolver to forward requests if the domain is not found in the master file (default: `10.2.1.10`).
- `MASTER_FILE_ROUTE`: The path to the master file that contains the domain-name to IP-address mappings (default: `masterFile.txt`).

## Functionality

- The server creates a `DatagramSocket` to listen for DNS requests.
- It receives a DNS request packet and extracts the domain name from it.
- It queries the master file to find the IP address corresponding to the domain name.
- If the IP address is found, it constructs a DNS response packet with the IP address.
- If the IP address is not found, it forwards the request to a foreign resolver.
- The server sends the DNS response back to the client.
