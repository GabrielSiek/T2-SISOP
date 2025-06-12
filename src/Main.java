public class Main {
    public static void main(String[] args) {
        try {
            SGMP sim =  new SGMP(
                    8,
                    6,
                    16,
                    4,
                    32,
                    16,
                    "input.txt");

            sim.run("output.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
