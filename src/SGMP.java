import java.io.*;
import java.util.*;

public class SGMP {

    private int VIRTUAL_MEMORY_SIZE; // memória virtual
    private int RAM_MEMORY_SIZE;     // memória física
    private int PAGE_AND_FRAME_SIZE; // tamanho da página
    private long SEG_TEXT, SEG_DATA, SEG_STACK, SEG_BSS; // segmentos
    private List<Long> V_ADDRS = new ArrayList<>(); // endereços virtuais

    private int[] PAGE_TABLE; // para 1 nível
    private Map<Integer, int[]> PAGE_TABLE_2LEVEL; // para 2 níveis
    private long[] INVERTED_PAGE_TABLE; // frameIndex → pageNumber (INVERTED correta!)
    private long[] FRAMES; // frames

    private PageTableType pageTableType;

    public SGMP(int vMem, int fMem, int pageAndFrameSize, int text, int data, int stack, String inputFile, PageTableType type) throws IOException {
        this.VIRTUAL_MEMORY_SIZE = (int) Math.pow(2, vMem);
        this.RAM_MEMORY_SIZE = (int) Math.pow(2, fMem);
        this.PAGE_AND_FRAME_SIZE = pageAndFrameSize;

        if (vMem < fMem) throw new IllegalArgumentException("Memória virtual deve ser maior ou igual que física.");

        this.SEG_TEXT = (long) Math.pow(2, text);
        this.SEG_DATA = (long) Math.pow(2, data);
        this.SEG_STACK = (long) Math.pow(2, stack);
        this.SEG_BSS = VIRTUAL_MEMORY_SIZE - (SEG_TEXT + SEG_DATA + SEG_STACK);

        this.pageTableType = type;

        readAddressesFromFile(inputFile);
        setupTables();
    }

    public void run(String outputFile) throws IOException {
        List<Long> physicalAddresses = new ArrayList<>();
        List<String> segments = new ArrayList<>();

        for (long vAddr : V_ADDRS) {
            segments.add(getSegment(vAddr));
            physicalAddresses.add(mapVirtualToPhysicalAddress(vAddr));
        }

        saveOutputToFile(outputFile, V_ADDRS, segments, physicalAddresses);
    }

    private void readAddressesFromFile(String path) throws IOException {
        long max = (long) VIRTUAL_MEMORY_SIZE;
        try (BufferedReader input = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = input.readLine()) != null) {
                long address = Long.parseLong(line.trim());
                if (address < 0 || address >= max)
                    throw new IllegalArgumentException("Endereço virtual inválido: " + address);
                V_ADDRS.add(address);
            }
        }
    }

    private void setupTables() {
        int pagesQuantity = VIRTUAL_MEMORY_SIZE / PAGE_AND_FRAME_SIZE;
        int framesQuantity = RAM_MEMORY_SIZE / PAGE_AND_FRAME_SIZE;

        switch (pageTableType) {
            case ONE_LEVEL:
                PAGE_TABLE = new int[pagesQuantity];
                Arrays.fill(PAGE_TABLE, -1);
                break;
            case TWO_LEVEL:
                PAGE_TABLE_2LEVEL = new HashMap<>();
                break;
            case INVERTED:
                INVERTED_PAGE_TABLE = new long[framesQuantity];
                Arrays.fill(INVERTED_PAGE_TABLE, -1); // -1 = moldura livre
                break;
        }

        FRAMES = new long[framesQuantity];
        Arrays.fill(FRAMES, -1);
    }

    private long mapVirtualToPhysicalAddress(long virtualAddress) {
        switch (pageTableType) {
            case ONE_LEVEL:
                return mapOneLevel(virtualAddress);
            case TWO_LEVEL:
                return mapTwoLevel(virtualAddress);
            case INVERTED:
                return mapInverted(virtualAddress);
            default:
                throw new IllegalStateException("Tipo de tabela não suportado.");
        }
    }

    private long mapOneLevel(long virtualAddress) {
        int pageIndex = (int) (virtualAddress / PAGE_AND_FRAME_SIZE);
        long offset = virtualAddress % PAGE_AND_FRAME_SIZE;

        int frameIndex = PAGE_TABLE[pageIndex];
        if (frameIndex == -1) {
            for (int i = 0; i < FRAMES.length; i++) {
                if (FRAMES[i] == -1) {
                    PAGE_TABLE[pageIndex] = i;
                    frameIndex = i;
                    break;
                }
            }
            if (frameIndex == -1) {
                System.out.println("Memória física cheia. Encerrando o simulador.");
                System.exit(0);
            }
        }

        FRAMES[frameIndex] = virtualAddress;
        return (long) frameIndex * PAGE_AND_FRAME_SIZE + offset;
    }

    private long mapTwoLevel(long virtualAddress) {
        int pageNumber = (int) (virtualAddress / PAGE_AND_FRAME_SIZE);
        int outerIndex = pageNumber / 256;
        int innerIndex = pageNumber % 256;
        long offset = virtualAddress % PAGE_AND_FRAME_SIZE;

        PAGE_TABLE_2LEVEL.putIfAbsent(outerIndex, new int[256]);
        int[] innerTable = PAGE_TABLE_2LEVEL.get(outerIndex);

        if (innerTable[innerIndex] == 0) {
            for (int i = 0; i < FRAMES.length; i++) {
                if (FRAMES[i] == -1) {
                    innerTable[innerIndex] = i + 1;
                    FRAMES[i] = virtualAddress;
                    return (long) i * PAGE_AND_FRAME_SIZE + offset;
                }
            }
            System.out.println("Memória física cheia. Encerrando o simulador.");
            System.exit(0);
        }

        int frameIndex = innerTable[innerIndex] - 1;
        return (long) frameIndex * PAGE_AND_FRAME_SIZE + offset;
    }

    private long mapInverted(long virtualAddress) {
        long pageNumber = virtualAddress / PAGE_AND_FRAME_SIZE;
        long offset = virtualAddress % PAGE_AND_FRAME_SIZE;

        // Verifica se a página já está em alguma moldura
        int frameIndex = -1;
        for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
            if (INVERTED_PAGE_TABLE[i] == pageNumber) {
                frameIndex = i;
                break;
            }
        }

        // Se não está, aloca em moldura livre
        if (frameIndex == -1) {
            for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
                if (INVERTED_PAGE_TABLE[i] == -1) {
                    INVERTED_PAGE_TABLE[i] = pageNumber;
                    frameIndex = i;
                    break;
                }
            }
            if (frameIndex == -1) {
                System.out.println("Memória física cheia. Encerrando o simulador.");
                System.exit(0);
            }
        }

        FRAMES[frameIndex] = virtualAddress;
        return (long) frameIndex * PAGE_AND_FRAME_SIZE + offset;
    }

    private String getSegment(long address) {
        if (address < SEG_TEXT) return ".text";
        else if (address < SEG_TEXT + SEG_DATA) return ".data";
        else if (address < SEG_TEXT + SEG_DATA + SEG_STACK) return ".stack";
        else if (address < VIRTUAL_MEMORY_SIZE) return ".bss";
        else return "unknown";
    }

    private void saveOutputToFile(String filename, List<Long> vAddrs, List<String> segments, List<Long> pAddrs) throws IOException {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println("EndereçoVirtual\tSegmento\tEndereçoFísico");
            for (int i = 0; i < vAddrs.size(); i++) {
                out.printf("%d\t\t%s\t\t%d\n", vAddrs.get(i), segments.get(i), pAddrs.get(i));
            }

            out.println("\nTabela de Páginas:");
            if (pageTableType == PageTableType.ONE_LEVEL && PAGE_TABLE != null) {
                for (int i = 0; i < PAGE_TABLE.length; i++) {
                    out.printf("Página %d → Moldura %d\n", i, PAGE_TABLE[i]);
                }
            } else if (pageTableType == PageTableType.TWO_LEVEL) {
                for (Map.Entry<Integer, int[]> entry : PAGE_TABLE_2LEVEL.entrySet()) {
                    int outer = entry.getKey();
                    int[] inner = entry.getValue();
                    for (int j = 0; j < inner.length; j++) {
                        if (inner[j] != 0)
                            out.printf("Página [%d][%d] → Moldura %d\n", outer, j, inner[j] - 1);
                    }
                }
            } else if (pageTableType == PageTableType.INVERTED) {
                for (int i = 0; i < INVERTED_PAGE_TABLE.length; i++) {
                    if (INVERTED_PAGE_TABLE[i] != -1) {
                        out.printf("Moldura %d → Página %d\n", i, INVERTED_PAGE_TABLE[i]);
                    } else {
                        out.printf("Moldura %d → (livre)\n", i);
                    }
                }
            }

            out.println("\nMemória Física:");
            out.println(Arrays.toString(FRAMES));
        }
    }
}
