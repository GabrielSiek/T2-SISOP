public class Main {
    public static void main(String[] args) {
        try {
            PaginacaoUmNivel umNivel = new PaginacaoUmNivel(
                    8, // virtual memory 256
                    6, // ram memory 64
                    16, // page and frame size
                    4,  // text size
                    32, // data size
                    16, // stack size
                    "input.txt");

            umNivel.run("1-um-nivel.txt");

            PaginacaoInvertida paginacaoInvertida = new PaginacaoInvertida(
                    8,
                    6,
                    16,
                    4,
                    32,
                    16,
                    "input.txt");

            paginacaoInvertida.run("3-invertida.txt");

            PaginacaoDoisNiveis doisNiveis = new PaginacaoDoisNiveis(
                    8,
                    6,
                    16,
                    4,
                    32,
                    16,
                    "input.txt");

            doisNiveis.run("2-dois-niveis.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
