import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 *  3‑D, colourful, slowly spinning “SUBHAN” rendered entirely in pure Java2D.
 *  ───────────────────────────────────────────────────────────────────────────
 *   • Rounded cubes (bevel + 2× inflate) instead of raw rectangles
 *   • Soft lighting (ambient + diffuse dot‑product)
 *   • Different colour for each letter
 *   • Smooth anti‑aliased rendering
 *   • Animation timer – no sliders required
 */
public class render {

    public static void main(String[] args) {

        JFrame frame = new JFrame("SUBHAN 3‑D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 640);
        frame.setLocationRelativeTo(null);

        /* store rotation in tiny array so inner lambdas can mutate it     */
        final double[] headingDeg = { 0 };
        final double[] pitchDeg   = { 0 };

        /* ───────────────── RENDER PANEL ───────────────── */
        JPanel canvas = new JPanel() {

            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);

                int W = getWidth(), H = getHeight();
                g2.setColor(Color.black);
                g2.fillRect(0, 0, W, H);

                /* ===== 1. BUILD LETTER GEOMETRY ================================= */
                List<Triangle> tris = new ArrayList<>();

                // 5×3 block grids for S‑U‑B‑H‑A‑N
                int[][][] letters = {
                    { {1,1,1},{1,0,0},{1,1,1},{0,0,1},{1,1,1} },   // S
                    { {1,0,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1} },   // U
                    { {1,1,0},{1,0,1},{1,1,0},{1,0,1},{1,1,0} },   // B
                    { {1,0,1},{1,0,1},{1,1,1},{1,0,1},{1,0,1} },   // H
                    { {0,1,0},{1,0,1},{1,1,1},{1,0,1},{1,0,1} },   // A
                    { {1,0,1},{1,1,1},{1,1,1},{1,1,1},{1,0,1} }    // N
                };

                Color[] palette = {
                    new Color(0xF44336),  // red
                    new Color(0xFF9800),  // orange
                    new Color(0xFFEB3B),  // yellow
                    new Color(0x4CAF50),  // green
                    new Color(0x2196F3),  // blue
                    new Color(0x9C27B0)   // purple
                };

                double block   = 32;                   // width / height of each cell
                double spacing = block * 4;            // gap between letters
                double depth   = 22;                   // extrusion thickness
                double bevel   = 4;                    // bevel amount

                double startX = -((letters.length - 1) * spacing) / 2.0;
                double startY = -block * 2.5;

                for (int li = 0; li < letters.length; li++) {
                    int[][] grid = letters[li];
                    Color col    = palette[li % palette.length];

                    for (int r = 0; r < grid.length; r++) {
                        for (int c = 0; c < grid[0].length; c++) {
                            if (grid[r][c] == 1) {
                                double x = startX + li * spacing + c * block;
                                double y = startY + r * block;
                                addRoundedCube(tris, x, y, block, block,
                                               depth, bevel, col);
                            }
                        }
                    }
                }

                /* 2× inflate gives nicely softened edges */
                for (int i = 0; i < 2; i++) tris = inflate(tris);

                /* ===== 2. BUILD TRANSFORM & BUFFERS ============================= */
                Matrix3 rotY = Matrix3.rotateY(Math.toRadians(headingDeg[0]));
                Matrix3 rotX = Matrix3.rotateX(Math.toRadians(pitchDeg[0]));
                Matrix3 transform = rotY.multiply(rotX);

                BufferedImage img = new BufferedImage(W, H,
                                                      BufferedImage.TYPE_INT_ARGB);
                double[] zBuf = new double[W * H];
                for (int i = 0; i < zBuf.length; i++) zBuf[i] = Double.NEGATIVE_INFINITY;

                /* light coming from camera */
                Vertex light = new Vertex(0, 0, 1);
                double ambient = 0.25;

                /* ===== 3. RASTERISE ============================================ */
                for (Triangle t : tris) {

                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);

                    v1.x += W / 2.0; v1.y += H / 2.0;
                    v2.x += W / 2.0; v2.y += H / 2.0;
                    v3.x += W / 2.0; v3.y += H / 2.0;

                    Vertex norm = Triangle.normal(v1, v2, v3);

                    double diffuse = Math.max(0, norm.dot(light));
                    double lumin   = ambient + (1 - ambient) * diffuse;

                    int minX = (int) Math.max(0,
                                  Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(W - 1,
                                  Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
                    int minY = (int) Math.max(0,
                                  Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(H - 1,
                                  Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double area = Triangle.area2D(v1, v2, v3);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {

                            double b1 = Triangle.bary(v2, v3, x, y, area);
                            double b2 = Triangle.bary(v3, v1, x, y, area);
                            double b3 = 1 - b1 - b2;

                            if (b1 >= 0 && b2 >= 0 && b3 >= 0) {
                                double zDepth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int idx = y * W + x;
                                if (zBuf[idx] < zDepth) {
                                    img.setRGB(x, y, shade(t.color, lumin).getRGB());
                                    zBuf[idx] = zDepth;
                                }
                            }
                        }
                    }
                }
                g2.drawImage(img, 0, 0, null);
            }
        };

        frame.add(canvas);
        frame.setVisible(true);

        /* ── ANIMATION TIMER ─────────────────────────────────────────────── */
        new Timer(16, e -> {
            headingDeg[0] = (headingDeg[0] + 0.7) % 360;
            pitchDeg[0]   = (pitchDeg[0] + 0.35) % 360;
            canvas.repaint();
        }).start();
    }

    /* ────────────────────────────────────────────────────────────────── */

    /** Adds a bevelled, extruded block (rounded cube). */
    static void addRoundedCube(List<Triangle> tris, double x, double y,
                               double sizeX, double sizeY,
                               double depth,  double bevel,
                               Color  colour) {

        int seg = 2;          // 2×2 subdivision -> inflate() will round edges
        double dx = sizeX / seg,  dy = sizeY / seg;

        for (int i = 0; i < seg; i++) {
            for (int j = 0; j < seg; j++) {

                boolean edge =
                    i == 0 || i == seg - 1 || j == 0 || j == seg - 1;

                double inset = edge ? bevel : 0;

                double px = x + i * dx + inset;
                double py = y + j * dy + inset;
                double sx = dx - 2 * inset;
                double sy = dy - 2 * inset;

                extrudeQuad(tris, px, py, sx, sy, depth, colour);
            }
        }
    }

    /** Extrude a single quad into a box (12 triangles). */
    private static void extrudeQuad(List<Triangle> tris,
                                    double x, double y,
                                    double w, double h,
                                    double d, Color c) {

        Vertex f1 = new Vertex(x,     y,     0),
               f2 = new Vertex(x + w, y,     0),
               f3 = new Vertex(x + w, y + h, 0),
               f4 = new Vertex(x,     y + h, 0);

        Vertex b1 = new Vertex(x,     y,     -d),
               b2 = new Vertex(x + w, y,     -d),
               b3 = new Vertex(x + w, y + h, -d),
               b4 = new Vertex(x,     y + h, -d);

        // front & back
        tris.add(new Triangle(f1, f2, f3, c)); tris.add(new Triangle(f1, f3, f4, c));
        tris.add(new Triangle(b3, b2, b1, c)); tris.add(new Triangle(b4, b3, b1, c));

        // sides
        tris.add(new Triangle(f1, f4, b4, c)); tris.add(new Triangle(f1, b4, b1, c)); // left
        tris.add(new Triangle(f2, b2, b3, c)); tris.add(new Triangle(f2, b3, f3, c)); // right
        tris.add(new Triangle(f4, f3, b3, c)); tris.add(new Triangle(f4, b3, b4, c)); // bottom
        tris.add(new Triangle(f2, f1, b1, c)); tris.add(new Triangle(f2, b1, b2, c)); // top
    }

    /** One pass of 1‑>4 triangle subdivision (no spherify). */
    static List<Triangle> inflate(List<Triangle> in) {
        List<Triangle> out = new ArrayList<>();
        for (Triangle t : in) {
            Vertex m1 = Vertex.mid(t.v1, t.v2),
                   m2 = Vertex.mid(t.v2, t.v3),
                   m3 = Vertex.mid(t.v1, t.v3);
            out.add(new Triangle(t.v1, m1, m3, t.color));
            out.add(new Triangle(t.v2, m1, m2, t.color));
            out.add(new Triangle(t.v3, m2, m3, t.color));
            out.add(new Triangle(m1,   m2, m3, t.color));
        }
        return out;
    }

    /* ── tiny helpers ───────────────────────────────────────────────── */

    static Color shade(Color base, double k) {
        k = Math.max(0, Math.min(1, k));
        return new Color((int)(base.getRed() * k),
                         (int)(base.getGreen() * k),
                         (int)(base.getBlue() * k));
    }

    /* ───────────────── Geometry classes ────────────────────────────── */

    static class Vertex {
        double x, y, z;
        Vertex(double x, double y, double z) { this.x=x; this.y=y; this.z=z; }
        static Vertex mid(Vertex a, Vertex b) {
            return new Vertex((a.x+b.x)/2, (a.y+b.y)/2, (a.z+b.z)/2);
        }
        double dot(Vertex o) { return x*o.x + y*o.y + z*o.z; }
    }

    static class Triangle {
        Vertex v1,v2,v3; Color color;
        Triangle(Vertex v1, Vertex v2, Vertex v3, Color clr){
            this.v1=v1; this.v2=v2; this.v3=v3; this.color=clr;
        }
        static Vertex normal(Vertex a, Vertex b, Vertex c){
            double ax=b.x-a.x, ay=b.y-a.y, az=b.z-a.z;
            double bx=c.x-a.x, by=c.y-a.y, bz=c.z-a.z;
            Vertex n = new Vertex(ay*bz-az*by, az*bx-ax*bz, ax*by-ay*bx);
            double len = Math.sqrt(n.x*n.x+n.y*n.y+n.z*n.z);
            n.x/=len; n.y/=len; n.z/=len;
            return n;
        }
        static double area2D(Vertex a, Vertex b, Vertex c){
            return (a.y - c.y)*(b.x - c.x) + (b.y - c.y)*(c.x - a.x);
        }
        static double bary(Vertex a, Vertex b, int x, int y, double area){
            return ((y - b.y)*(a.x - b.x) + (a.y - b.y)*(b.x - x)) / area;
        }
    }

    static class Matrix3 {
        final double[] m;
        Matrix3(double[] v){ m=v; }
        static Matrix3 rotateX(double a){
            return new Matrix3(new double[]{
                1,0,0,  0,Math.cos(a),Math.sin(a),  0,-Math.sin(a),Math.cos(a)});
        }
        static Matrix3 rotateY(double a){
            return new Matrix3(new double[]{
                Math.cos(a),0,-Math.sin(a),  0,1,0,  Math.sin(a),0,Math.cos(a)});
        }
        Matrix3 multiply(Matrix3 o){
            double[] r = new double[9];
            for(int r0=0;r0<3;r0++) for(int c=0;c<3;c++)
                for(int k=0;k<3;k++) r[r0*3+c]+=m[r0*3+k]*o.m[k*3+c];
            return new Matrix3(r);
        }
        Vertex transform(Vertex p){
            return new Vertex(
                p.x*m[0]+p.y*m[3]+p.z*m[6],
                p.x*m[1]+p.y*m[4]+p.z*m[7],
                p.x*m[2]+p.y*m[5]+p.z*m[8]);
        }
    }
}
