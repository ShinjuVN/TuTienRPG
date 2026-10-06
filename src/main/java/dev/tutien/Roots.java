package dev.tutien;

import java.util.*;

/** Linh can va The chat. */
public final class Roots {
    public record Root(String id, String name, double absorbMul, int weight) {}
    public record Body(String id, String name, double hpMul, int weight) {}

    public static final Map<String, Root> ROOTS = new LinkedHashMap<>();
    public static final Map<String, Body> BODIES = new LinkedHashMap<>();
    private static final Random RNG = new Random();

    static {
        r("kim", "Kim Linh Căn", 1.0, 20);
        r("moc", "Mộc Linh Căn", 1.0, 20);
        r("thuy", "Thủy Linh Căn", 1.0, 20);
        r("hoa", "Hỏa Linh Căn", 1.0, 20);
        r("tho", "Thổ Linh Căn", 1.0, 20);
        r("loi", "Lôi Linh Căn", 1.2, 8);
        r("bang", "Băng Linh Căn", 1.2, 8);
        r("phong", "Phong Linh Căn", 1.2, 8);
        r("thien", "Thiên Linh Căn", 1.6, 2);
        r("hon_don", "Hỗn Độn Linh Căn", 2.0, 1);

        b("pham_the", "Phàm Thể", 1.0, 60);
        b("kim_cang_the", "Kim Cang Thể", 1.2, 15);
        b("thai_duong_the", "Thái Dương Thể", 1.3, 8);
        b("thai_am_the", "Thái Âm Thể", 1.3, 8);
        b("hon_don_the", "Hỗn Độn Thể", 1.6, 1);
    }

    private Roots() {}

    private static void r(String id, String n, double m, int w) { ROOTS.put(id, new Root(id, n, m, w)); }
    private static void b(String id, String n, double m, int w) { BODIES.put(id, new Body(id, n, m, w)); }

    public static Root root(String id) { return ROOTS.getOrDefault(id, ROOTS.get("kim")); }
    public static Body body(String id) { return BODIES.getOrDefault(id, BODIES.get("pham_the")); }

    public static String randomRoot() {
        int total = ROOTS.values().stream().mapToInt(Root::weight).sum();
        int x = RNG.nextInt(total);
        for (Root r : ROOTS.values()) { x -= r.weight(); if (x < 0) return r.id(); }
        return "kim";
    }

    public static String randomBody() {
        int total = BODIES.values().stream().mapToInt(Body::weight).sum();
        int x = RNG.nextInt(total);
        for (Body r : BODIES.values()) { x -= r.weight(); if (x < 0) return r.id(); }
        return "pham_the";
    }
}
