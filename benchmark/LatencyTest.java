package benchmark;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// Do do tre va kiem tra he thong
public class LatencyTest {

    private static String GATEWAY_URL;
    private static String DIRECT_URL;

    private static String detectHost(int port, String path, String method) {
        String[] candidates = {"[::1]", "localhost", "127.0.0.1"};
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(800)).build();
        for (String host : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + host + ":" + port + path))
                        .timeout(Duration.ofMillis(800));
                if ("POST".equalsIgnoreCase(method)) {
                    builder.header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"cuongtm\",\"password\":\"123\"}"));
                } else {
                    builder.GET();
                }
                HttpResponse<String> res = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    return "http://" + host + ":" + port;
                }
            } catch (Exception ignored) {}
        }
        return "http://localhost:" + port;
    }

    public static void main(String[] args) throws Exception {
        GATEWAY_URL = detectHost(8080, "/users/login", "POST");
        DIRECT_URL = detectHost(8081, "/users/1", "GET");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        System.out.println("============================================================");
        System.out.println(" KIEM TRA VA DO DO TRE HE THONG ");
        System.out.println("============================================================");

        // 1. Dang nhap lay token qua Gateway
        System.out.println("\n1. Lay token qua Gateway endpoint /users/login");
        String loginJson = "{\"username\":\"cuongtm\",\"password\":\"123\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/users/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        if (loginRes.statusCode() != 200) {
            System.err.println(" -> Loi: Khong the dang nhap, ma loi " + loginRes.statusCode());
            return;
        }

        String body = loginRes.body();
        int tokenIdx = body.indexOf("\"token\":");
        String token = body.substring(tokenIdx + 9, body.indexOf("\"", tokenIdx + 9));
        System.out.println(" -> Dang nhap thanh cong, token: " + token.substring(0, 25) + "...");

        // 2. Kiem tra Aggregation gop thong tin
        System.out.println("\n2. Kiem tra Aggregation gop User va Orders");
        HttpRequest dashReq = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/dashboard?userId=1"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> dashRes = client.send(dashReq, HttpResponse.BodyHandlers.ofString());
        System.out.println(" -> Ma phan hoi " + dashRes.statusCode() + " | Ket qua: " + dashRes.body().substring(0, Math.min(80, dashRes.body().length())) + "...");

        // 3. Kiem tra Valet Key lay link upload avatar
        System.out.println("\n3. Kiem tra Valet Key lay link upload avatar");
        HttpRequest valetReq = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/users/1/avatar-presigned-url"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> valetRes = client.send(valetReq, HttpResponse.BodyHandlers.ofString());
        System.out.println(" -> Ma phan hoi " + valetRes.statusCode() + " | Link upload: " + valetRes.body().substring(0, Math.min(80, valetRes.body().length())) + "...");

        // 4. Kiem tra chan spam Rate Limit
        System.out.println("\n4. Kiem tra chan spam qua 10 request moi giay");
        int blocked = 0;
        for (int i = 0; i < 15; i++) {
            HttpRequest rlReq = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + "/users/1"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<Void> rlRes = client.send(rlReq, HttpResponse.BodyHandlers.discarding());
            if (rlRes.statusCode() == 429) {
                blocked++;
            }
        }
        System.out.println(" -> Gui 15 request lien tiep: " + blocked + " request bi Gateway chan ma 429");

        Thread.sleep(1500);

        // 5. Do do tre khi goi truc tiep va khi di qua Gateway
        System.out.println("\n5. Do do tre khi goi truc tiep va khi di qua Gateway");
        List<Double> directTimes = new ArrayList<>();
        List<Double> gatewayTimes = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            // Goi truc tiep
            long start1 = System.nanoTime();
            HttpRequest dReq = HttpRequest.newBuilder().uri(URI.create(DIRECT_URL + "/users/1")).GET().build();
            client.send(dReq, HttpResponse.BodyHandlers.discarding());
            directTimes.add((System.nanoTime() - start1) / 1_000_000.0);

            // Goi qua Gateway
            long start2 = System.nanoTime();
            HttpRequest gReq = HttpRequest.newBuilder().uri(URI.create(GATEWAY_URL + "/users/1")).header("Authorization", "Bearer " + token).GET().build();
            client.send(gReq, HttpResponse.BodyHandlers.discarding());
            gatewayTimes.add((System.nanoTime() - start2) / 1_000_000.0);

            Thread.sleep(120);
        }

        double avgDirect = directTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgGateway = gatewayTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double minDirect = directTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double minGateway = gatewayTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxDirect = directTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double maxGateway = gatewayTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        System.out.println("\n============================================================");
        System.out.println(" KET QUA DO DO TRE");
        System.out.println("============================================================");
        System.out.printf("%-20s | %-20s | %-18s%n", "Thong so ms", "Goi truc tiep", "Goi qua Gateway");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-20s | %-20.2f | %-18.2f%n", "Trung binh", avgDirect, avgGateway);
        System.out.printf("%-20s | %-20.2f | %-18.2f%n", "Nho nhat", minDirect, minGateway);
        System.out.printf("%-20s | %-20.2f | %-18.2f%n", "Lon nhat", maxDirect, maxGateway);
        System.out.println("------------------------------------------------------------");
        System.out.printf("-> Do tre chenh lech do Gateway xu ly: +%.2f ms%n", (avgGateway - avgDirect));
        System.out.println("============================================================");
    }
}
