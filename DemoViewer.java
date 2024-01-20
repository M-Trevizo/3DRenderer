import javax.swing.*;
import java.awt.*;
//import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DemoViewer {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // Slider for horizontal rotation
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // Slider for vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // Slider for roll rotation
        JSlider rollSlider = new JSlider(SwingConstants.VERTICAL, 0, 360, 180);
        pane.add(rollSlider, BorderLayout.WEST);

        // Panel to display render
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Create list of triangles describing Tetrahedron
                ArrayList<Triangle> triangles = new ArrayList<>();
                triangles.add(
                    new Triangle(
                        new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(-100, 100, -100),
                        Color.WHITE)
                );
                triangles.add(
                    new Triangle(
                        new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(100, -100, -100),
                        Color.RED)
                );
                triangles.add(
                    new Triangle(
                        new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(100, 100, 100),
                        Color.GREEN)
                );
                triangles.add(
                    new Triangle(
                        new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(-100, -100, 100),
                        Color.BLUE)
                );

                // Heading (XZ) transformation
                double heading = Math.toRadians(headingSlider.getValue());
                Matrix3 headingTransfom = new Matrix3(new double[] {
                    Math.cos(heading), 0, -Math.sin(heading),
                    0, 1, 0,
                    Math.sin(heading), 0, Math.cos(heading)
                });

                // Pitch (YZ) transformation
                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix3 pitchTransform = new Matrix3(new double[] {
                    1, 0, 0,
                    0, Math.cos(pitch), Math.sin(pitch),
                    0, -Math.sin(pitch), Math.cos(pitch)
                });

                // Roll (XY) transformation
                double roll = Math.toRadians(rollSlider.getValue());
                Matrix3 rollTransform = new Matrix3(new double[] {
                    Math.cos(roll), -Math.sin(roll), 0,
                    Math.sin(roll), Math.cos(roll), 0,
                    0, 0, 1
                });

                // Multiply all transformation matrices (heading * pitch * roll)
                // Results in a new matrix describing both transformation
                Matrix3 intermediateTransform = headingTransfom.multiply(pitchTransform);
                Matrix3 finalTransform = intermediateTransform.multiply(rollTransform);

                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                
                double[] zBuffer = new double[img.getWidth() * img.getHeight()];
                for(int i = 0; i < zBuffer.length; i++) {
                    zBuffer[i] = Double.NEGATIVE_INFINITY;
                }

                for(Triangle t : triangles) {
                    Vertex v1 = finalTransform.transform(t.v1);
                    Vertex v2 = finalTransform.transform(t.v2);
                    Vertex v3 = finalTransform.transform(t.v3);

                    // Manually apply transformation
                    v1.x += getWidth() / 2;
                    v1.y += getHeight() / 2;
                    v2.x += getWidth() / 2;
                    v2.y += getHeight() / 2;
                    v3.x += getWidth() / 2;
                    v3.y += getHeight() / 2;

                    // Calculate Normal for shading purposes
                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

                    Vertex norm = new Vertex(
                        ab.y * ac.z - ab.z * ac.y,
                        ab.z * ac.x - ab.x * ac.z,
                        ab.x * ac.y - ab.y * ac.x
                    );

                    double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;

                    // Drop the sign for demo but should be kept track of in order to shade accordingly
                    double angleCos = Math.abs(norm.z);

                    // Compute rectangular bounds for triangle
                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));

                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
                    
                    for(int y = minY; y <= maxY; y++) {
                        for(int x = minX; x <= maxX; x++) {
                            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                            
                            if(b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int zIndex = y * img.getWidth() + x;
                                
                                if(zBuffer[zIndex] < depth) {
                                    img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                                    zBuffer[zIndex] = depth;
                                }
                            }
                        }
                    }
                }
                g2d.drawImage(img, 0, 0, null);
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);
        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());
        rollSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(600, 600);
        frame.setVisible(true);
    }

    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.4);
        int green = (int) Math.pow(greenLinear, 1 / 2.4);
        int blue = (int) Math.pow(blueLinear, 1 / 2.4);

        return new Color(red, green, blue);
    }
}

class Vertex {
    
    double x;
    double y;
    double z;

    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

class Triangle {
    
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color color;

    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }
}

class Matrix3 {

    double[] values;

    Matrix3(double[] values) {
        this.values = values;
    }

    Matrix3 multiply(Matrix3 other) {
        double[] result = new double[9];
        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 3; col++) {
                for(int i = 0; i < 3; i++) {
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }
        return new Matrix3(result);
    }

    Vertex transform(Vertex in) {
        return new Vertex(
            in.x * values[0] + in.y * values[3] + in.z * values[6],
            in.x * values[1] + in.y * values[4] + in.z * values[7],
            in.x * values[2] + in.y * values[5] + in.z * values[8]);
    }
}