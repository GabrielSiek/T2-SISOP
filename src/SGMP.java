import java.io.*;
import java.util.*;

public class SGMP {

    private int V_MEM_SIZE;
    private int F_MEM_SIZE;
    private int PAGE_SIZE;
    private long SEG_TEXT, SEG_DATA, SEG_STACK, SEG_BSS;
    private List<Long> V_ADDRS = new ArrayList<>();
    private int[] PAGE_TABLE;
    private long[] F_MEM;

    public SGMP(int vMem, int fMem, int pageSize, int text, int data, int stack, String inputFile) throws IOException {
        this.V_MEM_SIZE = vMem;
        this.F_MEM_SIZE = fMem;
        this.PAGE_SIZE = pageSize;

        if (vMem <= fMem) throw new IllegalArgumentException("Memória virtual deve ser maior ou igual que física.");

        this.SEG_TEXT = (long) Math.pow(2, text);
        this.SEG_DATA = (long) Math.pow(2, data);
        this.SEG_STACK = (long) Math.pow(2, stack);
        this.SEG_BSS = SEG_TEXT + SEG_DATA + SEG_STACK;

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
        long max = (long) Math.pow(2, V_MEM_SIZE);
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
        int pagesAndFrames = (int) Math.pow(2, V_MEM_SIZE - PAGE_SIZE);
        PAGE_TABLE = new int[pagesAndFrames];
        F_MEM = new long[pagesAndFrames];
        Arrays.fill(PAGE_TABLE, -1);
        Arrays.fill(F_MEM, -1);
    }

    private long mapVirtualToPhysicalAddress(long virtualAddress) {
        long pageSize = (long) Math.pow(2, PAGE_SIZE);
        int pageIndex = (int) (virtualAddress / pageSize);
        long offset = virtualAddress % pageSize;

        int frameIndex = PAGE_TABLE[pageIndex];
        if (frameIndex == -1) {
            for (int i = 0; i < F_MEM.length; i++) {
                if (F_MEM[i] == -1) {
                    PAGE_TABLE[pageIndex] = i;
                    frameIndex = i;
                    break;
                }
            }
            if (frameIndex == -1)
                throw new IllegalStateException("Memória física cheia.");
        }

        F_MEM[frameIndex] = virtualAddress;
        return frameIndex * pageSize + offset;
    }

    private String getSegment(long address) {
        if (address < SEG_TEXT) return ".text";
        else if (address < SEG_TEXT + SEG_DATA) return ".data";
        else if (address < SEG_TEXT + SEG_DATA + SEG_STACK) return ".stack";
        else if (address < SEG_TEXT + SEG_DATA + SEG_STACK + SEG_BSS) return ".bss";
        else return "unknown";
    }

    private void saveOutputToFile(String filename, List<Long> vAddrs, List<String> segments, List<Long> pAddrs) throws IOException {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println("EndereçoVirtual\tSegmento\tEndereçoFísico");
            for (int i = 0; i < vAddrs.size(); i++) {
                out.printf("%d\t\t%s\t\t%d\n", vAddrs.get(i), segments.get(i), pAddrs.get(i));
            }
            out.println("\nTabela de Páginas:");
            out.println(Arrays.toString(PAGE_TABLE));
            out.println("\nMemória Física:");
            out.println(Arrays.toString(F_MEM));
        }
    }
}
