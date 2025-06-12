public class Main {
    public static void main(String[] args) {
        try {
            PaginacaoUmNivel umNivel =  new PaginacaoUmNivel(
                    8, //virtual memory 256
                    6, //ram memory 64
                    16, //page and frame size
                    4,  //text size
                    32, //data size
                    16, //stack size
                    "input.txt");

            umNivel.run("um-nivel.txt");

            PaginacaoInvertida paginacaoInvertida = new PaginacaoInvertida(
                    8, //virtual memory 256
                    6, //ram memory 64
                    16, //page and frame size
                    4,  //text size
                    32, //data size
                    16, //stack size
                    "input.txt");

            paginacaoInvertida.run("invertida.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
