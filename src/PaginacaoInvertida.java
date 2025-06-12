import java.io.*;
import java.util.*;

public class PaginacaoInvertida {

    private int VIRTUAL_MEMORY_SIZE;
    private int RAM_MEMORY_SIZE;
    private int PAGE_AND_FRAME_SIZE;
    private long SEG_TEXT, SEG_DATA, SEG_STACK, SEG_BSS;
    private List<Long> V_ADDRS = new ArrayList<>();
    private InvertedPageTableEntry[] INVERTED_PAGE_TABLE;
    private long[] FRAMES;

    // Classe interna para representar cada entrada da tabela invertida
    private static class InvertedPageTableEntry {
        long virtualPageNumber;
        String segment;
        boolean valid;

        InvertedPageTableEntry() {
            this.valid = false;
        }

        InvertedPageTableEntry(long vpn, String segment) {
            this.virtualPageNumber = vpn;
            this.segment = segment;
            this.valid = true;
        }
    }

    public PaginacaoInvertida(int vMem, int fMem, int pageAndFrameSize, int text, int data, int stack, String inputFile) throws IOException {
        this.VIRTUAL_MEMORY_SIZE = (int) Math.pow(2, vMem);
        this.RAM_MEMORY_SIZE = (int) Math.pow(2, fMem);
        this.PAGE_AND_FRAME_SIZE = pageAndFrameSize;

        if (vMem < fMem) throw new IllegalArgumentException("Memória virtual deve ser maior ou igual que física.");

        this.SEG_TEXT = (long) Math.pow(2, text);
        this.SEG_DATA = data;
        this.SEG_STACK = stack;
        this.SEG_BSS = VIRTUAL_MEMORY_SIZE - (SEG_TEXT + SEG_DATA + SEG_STACK);

        readAddressesFromFile(inputFile);
        setupTables();
    }

    public void run(String outputFile) throws IOException {
        List<Long> physicalAddresses = new ArrayList<>();
        List<String> segments = new ArrayList<>();

        for (long vAddr : V_ADDRS) {
            String segment = getSegment(vAddr);
            segments.add(segment);
            physicalAddresses.add(mapVirtualToPhysicalAddress(vAddr, segment));
        }

        saveOutputToFile(outputFile, V_ADDRS, segments, physicalAddresses);
    }

    private void readAddressesFromFile(String path) throws IOException {
        try (BufferedReader input = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = input.readLine()) != null) {
                long address = Long.parseLong(line.trim());
                if (address < 0 || address >= VIRTUAL_MEMORY_SIZE)
                    throw new IllegalArgumentException("Endereço virtual inválido: " + address);
                V_ADDRS.add(address);
            }
        }
    }

    private void setupTables() {
        int framesQuantity = RAM_MEMORY_SIZE / PAGE_AND_FRAME_SIZE;
        INVERTED_PAGE_TABLE = new InvertedPageTableEntry[framesQuantity];
        FRAMES = new long[framesQuantity];

        for (int i = 0; i < framesQuantity; i++) {
            INVERTED_PAGE_TABLE[i] = new InvertedPageTableEntry();
            FRAMES[i] = -1;
        }
    }

    private long mapVirtualToPhysicalAddress(long virtualAddress, String segment) {
        long pageIndex = virtualAddress / PAGE_AND_FRAME_SIZE;
        long offset = virtualAddress % PAGE_AND_FRAME_SIZE;

        // Verifica se a página já está mapeada
        for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
            InvertedPageTableEntry entry = INVERTED_PAGE_TABLE[i];
            if (entry.valid && entry.virtualPageNumber == pageIndex && entry.segment.equals(segment)) {
                return i * PAGE_AND_FRAME_SIZE + offset;
            }
        }

        // Procura um frame livre
        for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
            if (!INVERTED_PAGE_TABLE[i].valid) {
                INVERTED_PAGE_TABLE[i] = new InvertedPageTableEntry(pageIndex, segment);
                FRAMES[i] = virtualAddress;
                return i * PAGE_AND_FRAME_SIZE + offset;
            }
        }

        throw new IllegalStateException("Memória física cheia.");
    }

    private String getSegment(long address) {
        if (address < SEG_TEXT) return ".text";
        else if (address < SEG_TEXT + SEG_DATA) return ".data";
        else if (address < SEG_TEXT + SEG_DATA + SEG_STACK) return ".stack";
        return ".bss";
    }

    private void saveOutputToFile(String filename, List<Long> vAddrs, List<String> segments, List<Long> pAddrs) throws IOException {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println("EndereçoVirtual\tSegmento\tEndereçoFísico");
            for (int i = 0; i < vAddrs.size(); i++) {
                out.printf("%d\t\t\t%s\t\t\t%d\n", vAddrs.get(i), segments.get(i), pAddrs.get(i));
            }

            out.println("\nTabela de Páginas Invertida:");
            for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
                InvertedPageTableEntry entry = INVERTED_PAGE_TABLE[i];
                if (entry.valid) {
                    out.printf("Frame %d -> Página Virtual %d (%s)\n", i, entry.virtualPageNumber, entry.segment);
                } else {
                    out.printf("Frame %d -> LIVRE\n", i);
                }
            }

            out.println("\nMemória Física:");
            out.println(Arrays.toString(FRAMES));
        }
    }
}
