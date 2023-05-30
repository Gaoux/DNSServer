import java.io.*;
import java.net.*;
import java.util.*;


public class DNSServer {
    private static final int DNS_PORT = 53;
    private static final String FOREIGN_RESOLVER_IP = "10.2.1.10"; //IP UNIVERSIDAD
    private static final String MASTER_FILE_ROUTE = "masterFile.txt";// Dir File

    public static void main(String[] args) {
        try {
            // Create a DatagramSocket to listen for DNS requests
            DatagramSocket socket = new DatagramSocket(DNS_PORT);

            System.out.println("DNS server started...\n");
            while (true) {
                // Create a byte array to hold the incoming DNS request
                byte[] requestData = new byte[1024];
                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length);
                // Receive the DNS request
                socket.receive(requestPacket);
                System.out.println("Connection established");
                // Construct the DNS response
                byte[] responsePacket = handleDNSRequest(requestPacket.getData(), requestPacket.getLength());

                // Send the DNS response back to the client
                DatagramPacket response = new DatagramPacket(responsePacket, responsePacket.length,
                        requestPacket.getAddress(), requestPacket.getPort());
                socket.send(response);
                System.out.println("Response sent to " + requestPacket.getAddress() + ":" + requestPacket.getPort() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static byte[] handleDNSRequest(byte[] requestData, int requestPacketLength) throws IOException {
        // Extract domain name from the DNS request
        String domainName = extractDomainName(requestData, 12);
        //Get the IP address of the domain
        InetAddress ipAddress = queryIPAddress(domainName);
        //If ipAddress null it means that the master file doesn't have the domain, so its forward to a foreign resolver
        System.out.println("Domain: " + domainName);
        if (ipAddress == null)
            return forwardToForeignResolver(requestData,requestPacketLength);
        System.out.println(" IP address: " + ipAddress);
        // Return the DNS response packet with the IP address
        return constructDNSResponse(requestData,requestPacketLength, ipAddress);
    }
    private static InetAddress queryIPAddress(String domainName) throws IOException {
        File file = new File(MASTER_FILE_ROUTE);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();

        while (line != null) {
            StringTokenizer st = new StringTokenizer(line, ":");
            String domain = st.nextToken().trim();

            if(domainName.equals(domain))
                try {
                    return InetAddress.getByName(st.nextToken().trim());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            line = br.readLine();
        }
        return null;
    }
    private static byte[] constructDNSResponse(byte[] requestData, int requestPacketLength, InetAddress ipAddress) {
        byte[] responseData = new byte[requestPacketLength + 16];

        // Copy the original response packet to the new response packet
        System.arraycopy(requestData, 0, responseData, 0, requestPacketLength);

        responseData[2] |= (byte) 0x80; // Set the response flag to 1 (response)
        responseData[3] |= (byte) 0x80; // Set the authoritative answer flag to 1 (true)
        responseData[7] = 0x01; // Set the answer count to 1

        // Answer section
        int offset = requestPacketLength;
        responseData[offset++] = (byte) 0xC0;
        responseData[offset++] = 0x0C; // Name pointer to original name in the query section
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x01; // Type: A record (IPv4 address)
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x01; // Class: IN (Internet)
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x78; // 0x78 represents 120 in hexadecimal
        responseData[offset++] = 0x00;
        responseData[offset++] = 0x04; // Resource data length (IP address length), since it's IPv4 its length is 4 bytes (0x04)

        byte[] ipBytes = ipAddress.getAddress();
        System.arraycopy(ipBytes, 0, responseData, offset, 4);

        return responseData;
    }
    private static String extractDomainName(byte[] message, int offset) {
        StringBuilder domainName = new StringBuilder();
        int currentPosition = offset;
        int length = message[currentPosition];
        while ( length != 0) {
            for (int i = currentPosition + 1; i <= currentPosition+ length; i++)
                domainName.append((char) message[i]);

            currentPosition += length + 1;
            length = message[currentPosition];

            if (length != 0)
                domainName.append('.');
        }
        return domainName.toString().toLowerCase();
    }
    private static byte[] forwardToForeignResolver(byte[] requestData, int requestPackageLength) {
        try {
            DatagramSocket socket = new DatagramSocket();

            DatagramPacket requestPacket = new DatagramPacket(
                    requestData,
                    requestPackageLength,
                    InetAddress.getByName(FOREIGN_RESOLVER_IP),
                    DNS_PORT
            );
            socket.send(requestPacket);
            //System.out.println(requestPacket);
            byte[] responseBuffer = new byte[1024]; // Maximum DNS message size
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);
            // Extract the IP address from the response packet and return it
            // Modify the response handling based on the DNS protocol specification (RFC 1035)
            return responseBuffer;
        } catch (Exception e) {
            e.printStackTrace();
        }
            return null; // Return null if the IP address cannot be resolved by the foreign resolver
    }
}
