import java.io.*;
import java.util.*;

public class PaginacaoDoisNiveis {
    private final int VIRTUAL_MEMORY_SIZE;
    private final int RAM_MEMORY_SIZE;
    private final int PAGE_AND_FRAME_SIZE;
    private final int TEXT_SIZE;
    private final int DATA_SIZE;
    private final int STACK_SIZE;
    private final String INPUT_FILE;

    private final int[] SEGMENTS_BASE;
    private final long[] FRAMES;
    private final Map<Integer, int[]> PAGE_TABLE_2LEVEL;
    private final List<Long> addresses = new ArrayList<>();

    public PaginacaoDoisNiveis(int virtualMemorySize, int ramMemorySize, int pageAndFrameSize,
                               int textSize, int dataSize, int stackSize, String inputFile) {
        this.VIRTUAL_MEMORY_SIZE = virtualMemorySize;
        this.RAM_MEMORY_SIZE = ramMemorySize;
        this.PAGE_AND_FRAME_SIZE = pageAndFrameSize;
        this.TEXT_SIZE = textSize;
        this.DATA_SIZE = dataSize;
        this.STACK_SIZE = stackSize;
        this.INPUT_FILE = inputFile;

        int framesQuantity = (int) Math.pow(2, RAM_MEMORY_SIZE) / PAGE_AND_FRAME_SIZE;
        this.FRAMES = new long[framesQuantity];
        Arrays.fill(FRAMES, -1);

        this.PAGE_TABLE_2LEVEL = new HashMap<>();

        this.SEGMENTS_BASE = new int[]{
                0,                                 // Código
                TEXT_SIZE * PAGE_AND_FRAME_SIZE,   // Dados
                (int) Math.pow(2, VIRTUAL_MEMORY_SIZE) - STACK_SIZE * PAGE_AND_FRAME_SIZE // Pilha
        };
    }

    public void run(String outputFile) throws IOException {
        setupTables();
        readAddressesFromFile();
        saveOutputToFile(outputFile);
    }

    private void setupTables() {
        // Pode ser expandido se necessário para simular carregamento inicial
    }

    private void readAddressesFromFile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                addresses.add(Long.parseLong(line.trim()));
            }
        }
    }

    private long mapVirtualToPhysicalAddress(long virtualAddress) {
        int pageNumber = (int) (virtualAddress / PAGE_AND_FRAME_SIZE);
        int outerIndex = pageNumber / VIRTUAL_MEMORY_SIZE;
        int innerIndex = pageNumber % VIRTUAL_MEMORY_SIZE;
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
            throw new IllegalStateException("Memória física cheia.");
        }

        int frameIndex = innerTable[innerIndex] - 1;
        return (long) frameIndex * PAGE_AND_FRAME_SIZE + offset;
    }

    private String getSegment(long virtualAddress) {
        if (virtualAddress < SEGMENTS_BASE[1]) return "TEXTO";
        else if (virtualAddress < SEGMENTS_BASE[2]) return "DADOS";
        else return "PILHA";
    }

    private void saveOutputToFile(String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (long address : addresses) {
                long physical = mapVirtualToPhysicalAddress(address);
                String segment = getSegment(address);
                writer.write(String.format("%s - VA: %d -> PA: %d", segment, address, physical));
                writer.newLine();
            }

            writer.newLine();
            writer.write("TABELA DE PÁGINAS (DOIS NÍVEIS):");
            writer.newLine();
            for (Map.Entry<Integer, int[]> entry : PAGE_TABLE_2LEVEL.entrySet()) {
                int outer = entry.getKey();
                int[] inner = entry.getValue();
                for (int j = 0; j < inner.length; j++) {
                    if (inner[j] != 0) {
                        writer.write(String.format("Página [%d][%d] → Moldura %d", outer, j, inner[j] - 1));
                        writer.newLine();
                    }
                }
            }

            writer.newLine();
            writer.write("MEMÓRIA FÍSICA:");
            writer.newLine();
            writer.write(Arrays.toString(FRAMES));
        }
    }
}
