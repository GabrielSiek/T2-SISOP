import java.io.*;
import java.util.*;

public class SGMP {

    private int VIRTUAL_MEMORY_SIZE; //memoria virtual
    private int RAM_MEMORY_SIZE; //memoria fisica
    private int PAGE_AND_FRAME_SIZE; //pagina
    private long SEG_TEXT, SEG_DATA, SEG_STACK, SEG_BSS; //segmentos
    private List<Long> V_ADDRS = new ArrayList<>(); //enderços virtuais
    private int[] PAGE_TABLE; //paginas
    private long[] FRAMES; //frames

    public SGMP(int vMem, int fMem, int pageAndFrameSize, int text, int data, int stack, String inputFile) throws IOException {
        this.VIRTUAL_MEMORY_SIZE = (int) Math.pow(2, vMem);
        this.RAM_MEMORY_SIZE = (int) Math.pow(2, fMem);
        this.PAGE_AND_FRAME_SIZE = pageAndFrameSize;

        if (vMem < fMem) throw new IllegalArgumentException("Memória virtual deve ser maior ou igual que física.");

        this.SEG_TEXT = (long) Math.pow(2, text);
        this.SEG_DATA = data;
        this.SEG_STACK = stack;
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
        long max = (long) Math.pow(2, VIRTUAL_MEMORY_SIZE);
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
        PAGE_TABLE = new int[pagesQuantity];
        FRAMES = new long[framesQuantity];
        Arrays.fill(PAGE_TABLE, -1);
        Arrays.fill(FRAMES, -1);
    }

    private long mapVirtualToPhysicalAddress(long virtualAddress) {
        //calcula index e offset
        int pageIndex = (int) (virtualAddress / PAGE_AND_FRAME_SIZE);
        long offset = virtualAddress % PAGE_AND_FRAME_SIZE;

        //pega o frame index a partir do index
        int frameIndex = PAGE_TABLE[pageIndex];

        //frameIndex = -1 -> pagina vazia
        if (frameIndex == -1) {

            //percorre todos os frames até encontrar um livre
            for (int i = 0; i < FRAMES.length; i++) {

                //se estiver livre aponta a pagina para o frame i
                //tbm salva o valor do frameindex pra i
                if (FRAMES[i] == -1) {
                    PAGE_TABLE[pageIndex] = i;
                    frameIndex = i;
                    break;
                }
            }
            if (frameIndex == -1)
                throw new IllegalStateException("Memória física cheia.");
        }

        FRAMES[frameIndex] = virtualAddress;
        return (long) frameIndex * PAGE_AND_FRAME_SIZE + offset;
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
            out.println(Arrays.toString(FRAMES));
        }
    }
}
