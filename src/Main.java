public class Main {
    public static void main(String[] args) {
        try {
            SGMP sim =  new SGMP(
                    8, //virtual memory 256
                    6, //ram memory 64
                    16, //page and frame size
                    4,  //text size
                    32, //data size
                    16, //stack size
                    "input.txt");

            sim.run("output.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
