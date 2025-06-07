public class Main {
    public static void main(String[] args) {
        try {
            SGMP sim = new SGMP(
                    14,       // V_MEM_SIZE
                    13,       // F_MEM_SIZE
                    11,       // PAGE_SIZE
                    9,        // .text
                    8,        // .data
                    7,        // .stack
                    "input.txt"
            );

            sim.run("output.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
