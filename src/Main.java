public static void main(String[] args) {
    try {
        SGMP sim = new SGMP(
                8,  // bits da memória virtual → 2^8 = 256 bytes
                6,  // bits da memória física → 2^6 = 64 bytes
                16, // tamanho da página em bytes

                4,  // .text → 2^4 = 16 bytes
                5,  // .data → 2^5 = 32 bytes
                4,  // .stack → 2^4 = 16 bytes

                "input.txt",
                PageTableType.ONE_LEVEL// você pode testar com TWO_LEVEL ou INVERTED
        );

        sim.run("output.txt");

        System.out.println("Simulação concluída. Resultados salvos em output.txt");

    } catch (Exception e) {
        e.printStackTrace();
    }
}
