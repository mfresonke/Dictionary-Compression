package com.maxfresonke.cda4630;


// Created by Maxwell Fresonke on 3/23/17
// MIT Licensed
// On my honor, I have neither given nor received unauthorized aid on this assignment

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SIM {
    /* Debug Settings */
    private static final boolean DEBUG_OUT = true;

    /* Helper Constants */
    private static final String FILENAME_COMPRESSION_INPUT = "original.txt";
    private static final String FILENAME_COMPRESSION_OUTPUT = "cout.txt";
    private static final String FILENAME_DECOMPRESSION_INPUT = "compressed.txt";
    private static final String FILENAME_DECOMPRESSION_OUTPUT = "dout.txt";

    private static final String FLAG_COMPRESS = "1";
    private static final String FLAG_DECOMPRESS = "2";

    private static final int FORMAT_BITS = 3; // num bits that denote the format (or strategy)
    private static final int OUTPUT_WIDTH = 32;

    private static final int DICTIONARY_SIZE = 16;
    private static final int DICTIONARY_NUM_BITS = 16;
    private static final String DICTIONARY_SEPARATOR = "xxxx";

    public static void main(String[] args) throws IOException {

        // TODO don't use testing config
        runTesting();

        /*

        // Parse Input Args
        if (args.length != 1) {
            System.err.println("Please enter the correct number of arguments");
            System.exit(2);
        }

        final String compOrDecompArg = args[0];
        String output = "";


        try {
            if (compOrDecompArg.equals(FLAG_COMPRESS)) {
                CompressionInput compIn = new CompressionInput(FILENAME_COMPRESSION_INPUT);
                output = runCompression(compIn);
            } else if (compOrDecompArg.equals(FLAG_DECOMPRESS)) {
                DecompressionInput decompIn = new DecompressionInput(FILENAME_DECOMPRESSION_INPUT,
                output = runDecompression();
            } else {
                System.err.println("Error, unrecognized argument '"+compOrDecompArg+"'.");
                System.exit(4);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }


        // TODO Write to file
        //DEBUG
        System.out.println(output);
        //END DEBUG

        */
    }

    private static void runTesting() throws IOException {

        // Compression strategies in order of priority
        List<CompressionStrategy> compStrategies = Arrays.asList(
                new OriginalBinaryEncodingStrategy(),
                new RunLengthEncodingStrategy(),
                null,   // Bitmask-Based Compression
                null,   // 1-bit Mismatch
                null,   // 2-bit consecutive mismatch
                null,   // 4-bit consecutive mismatch
                null,   // 2-bit anywhere mismatch
                null    // directMatch
        );

        CompressionInput compIn = new CompressionInput(FILENAME_COMPRESSION_INPUT);
        String compOut = runCompression(compIn);
        System.out.println("=================== COMPRESSION OUT ===================");
        System.out.println(compOut);
        System.out.println("=================== END COMPRESSION OUT ===================");

        String[] compOutLines = compOut.split("\n");
        DecompressionInput decompIn = new DecompressionInput(Arrays.asList(compOutLines), compStrategies, DICTIONARY_SEPARATOR, FORMAT_BITS);
        String decompOut = runDecompression(decompIn);

        System.out.println("=================== DECOMPRESSION OUT ===================");
        System.out.println(decompOut);
        System.out.println("=================== END DECOMPRESSION OUT ===================");
    }

    private static String runCompression(CompressionInput input, List<CompressionStrategy> compStrategies) throws IOException {

        CompressionOutputBuilder outputBuilder = new CompressionOutputBuilder(FORMAT_BITS, OUTPUT_WIDTH, DEBUG_OUT);

        // Run a pass over time file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(input, DICTIONARY_NUM_BITS, DICTIONARY_SIZE);

        // run compressions
        // for each uncompressed input line...
        for (int line=0; line!=input.size(); ++line) {
            // find the most efficient compression mechanism.
            int bestMethodFormat = -1;
            int bestMethodBitsUsed = Integer.MAX_VALUE;
            int bestMethodLinesConsumed = 0;
            String bestMethodOutput = "";
            for (int compStratI = 0; compStratI != compStrategies.size(); ++compStratI) {
                CompressionStrategy strategy = compStrategies.get(compStratI);
                // TODO remove after every method is implemented
                if (strategy == null) {
                    continue;
                }
                CompressionResult result = strategy.compress(dict, input, line);
                // if that particular strategy could not compress the input, skip it.
                if (result == null) {
                    continue;
                }
                int bitsUsed = strategy.getEncodingLength();
                int linesConsumed = result.getLinesConsumed();
                String compressedLine = result.getCompressedLine();
                if (bitsUsed < bestMethodBitsUsed) {
                    bestMethodFormat = compStratI;
                    bestMethodBitsUsed = bitsUsed;
                    bestMethodLinesConsumed = linesConsumed;
                    bestMethodOutput = compressedLine;
                }
            }
            // awesome! we found the best compression method.
            outputBuilder.add()
        }


        // create final output, inc. dictionary
        StringBuilder output = new StringBuilder();
        output.append(outputBuilder.toString());
        output.append(DICTIONARY_SEPARATOR);
        output.append("\n");
        output.append(dict.toString());

        return output.toString();
    }

    private static String runDecompression(DecompressionInput input) {
        StringBuilder output = new StringBuilder();

        // Run a pass over time file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(input, DICTIONARY_NUM_BITS);
        return output.toString();
    }

}

class Formatter {
    /**
     * Generates a binary string with the specified number of bits
     * @param num
     * @param numBits
     * @return
     */
    public static String genBinaryString(int num, int numBits) {
        String raw = Integer.toBinaryString(num);
        if (raw.length() > numBits) {
            throw new IllegalArgumentException("num greater than bits can hold");
        } else if (raw.length() == numBits) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(numBits);
        int zerosNeeded = numBits - raw.length();
        for (int i=0; i!=zerosNeeded; ++i) {
            sb.append("0");
        }
        sb.append(raw);
        return sb.toString();
    }
}


/* ======= Output Builders ======= */

class CompressionOutputBuilder {
    //private List<String> uncompressedInstructions = new LinkedList<>();
    private final List<String> compressedInstructions = new LinkedList<>();
    private final int FORMAT_BITS;
    private final int OUTPUT_WIDTH;
    private final boolean DEBUG_OUT; // creates a line after every instruction instead of every OUTPUT_WIDTH bits

    CompressionOutputBuilder(int formatBits, int outputWidth, boolean debugOut) {
        this.FORMAT_BITS = formatBits;
        this.OUTPUT_WIDTH = outputWidth;
        this.DEBUG_OUT = debugOut;
    }
    public void add(int format, String compressedLine) {
        String toAdd = Formatter.genBinaryString(format, FORMAT_BITS) + compressedLine;
        compressedInstructions.add(toAdd);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (DEBUG_OUT) {
            for (String instruction : compressedInstructions) {
                sb.append(instruction);
                sb.append("\n");
            }
        } else {
            // will hold giant list of the instructions with no spacing
            StringBuilder temp = new StringBuilder();
            for (String instruction : compressedInstructions) {
                temp.append(instruction);
            }
            // now split by OUTPUT_WIDTH
            int index = 0;
            while (index < temp.length()) {
                sb.append(temp.substring(index, Math.min(index + 4,temp.length())));
                sb.append("\n");
                index += OUTPUT_WIDTH;
            }
        }
        return sb.toString();
    }
}

class DecompressionOutputBuilder {
    List<String> instructions = new LinkedList<>();
    public void add(String instruction) {
        instructions.add(instruction);
    }
    public String previousInstruction() {
        return instructions.get(instructions.size()-1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(String instruction : instructions) {
            sb.append(instruction);
        }
        return sb.toString();
    }
}

class CompressionResult {
    private int linesConsumed;
    private String compressedLine;

    public CompressionResult(String compressedLine, int linesConsumed) {
        this.linesConsumed = linesConsumed;
        this.compressedLine = compressedLine;
    }

    public int getLinesConsumed() {
        return linesConsumed;
    }
    public String getCompressedLine() {
        return compressedLine;
    }
}

interface CompressionStrategy {
    int getEncodingLength();
    CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress);
    void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String lineToDecompress);
}

class OriginalBinaryEncodingStrategy implements CompressionStrategy {

    private static final int ENCODING_LEN = 32;

    @Override
    public int getEncodingLength() {
        return ENCODING_LEN;
    }

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        return new CompressionResult(input.getLine(inputToCompress), 1);
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String lineToDecompress) {
        outputBuilder.add(lineToDecompress);
    }
}

class RunLengthEncodingStrategy implements CompressionStrategy {

    private static final int LEN_ENCODING = 3;

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        // if the index is zero, it means we can't possibly look back! Return null meaning "can't compress"
        if (inputToCompress == 0) {
            return null;
        }
        // next we want to check if this encoding strategy even applies.
        // to do that, we look back on the previous line and determine whether
        //  it matches the current one we are trying to compress.
        String currLine = input.getLine(inputToCompress);
        String lastLine = input.getLine(inputToCompress - 1);
        // let's go ahead and eliminate the case where the input does __not__ match.
        if (!lastLine.equals(currLine)) {
            // RLE doesn't apply, so...
            return null;
        }
        // ok cool! We're almost there. Now for the fun stuff!
        // we need to look __ahead__ and see if any future lines
        //  can also be compressed. Isn't RLE fun?! :D
        int lookAheadCount = 0;
        for (int i = inputToCompress + 1; i < input.size(); ++i) {
            if (input.getLine(i).equals(currLine)) {
                ++lookAheadCount;
            } else {
                break;
            }
        }
        // lookAheadCount + 1 since this we must count the current line, too.
        int linesConsumed = lookAheadCount + 1;
        String output = Formatter.genBinaryString(lookAheadCount, LEN_ENCODING);
        return new CompressionResult(output, linesConsumed);
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String lineToDecompress) {
        // parse the number of times to repeat (+1 due to zero indexing)
        final int timesToRepeat = Integer.parseInt(lineToDecompress, 2) + 1;
        // get prev val to duplicate
        String toDupe = outputBuilder.previousInstruction();
        // remove item at index
        for (int i = 0; i != timesToRepeat; ++i) {
            outputBuilder.add(toDupe);
        }
    }

    @Override
    public int getEncodingLength() {
        return LEN_ENCODING;
    }
}


/* Helper Types */

class Dictionary {
    private final int NUM_BITS;
    final List<String> instructions;

    // arbitrarily large number to help with sorting
    private static final int MAX_ENTRIES = 10000;

    private static class InstructionEntry implements Comparable<InstructionEntry> {
        private int index;
        private String instruction;
        private int count = 0;


        public InstructionEntry(String instruction, int index) {
            this.index = index;
            this.instruction = instruction;
        }

        public void incrementCount() {
            ++count;
        }

        public int getIndex() {
            return index;
        }

        public int getCount() {
            return count;
        }

        public String getInstruction() {
            return instruction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InstructionEntry that = (InstructionEntry) o;

            if (index != that.index) return false;
            if (count != that.count) return false;
            return instruction.equals(that.instruction);

        }

        @Override
        public int hashCode() {
            int result = index;
            result = 31 * result + instruction.hashCode();
            result = 31 * result + count;
            return result;
        }

        @Override
        public int compareTo(InstructionEntry o) {
            // sort in DEscending order for the number of times the thing occured
            int countDiff = (o.count - count) * MAX_ENTRIES;
            // but in AScending order by index!
            countDiff += (index - o.index);
            return countDiff;
        }
    }

    /**
     * Constructor for when decompressing an already existing dictionary
     * @param input
     * @param numBits
     */
    public Dictionary(DecompressionInput input, int numBits) {
        NUM_BITS = numBits;
        if (input.rawDictSize() < 0 || input.rawDictSize() > (int) Math.pow(2, NUM_BITS)) {
            throw new IllegalArgumentException("invalid dict size");
        }
        instructions = new ArrayList<>(input.rawDictSize());
        for (int i = 0; i!=input.rawDictSize(); ++i) {
            instructions.add(input.getRawDict(i));
        }
    }

    /**
     * Constructor for when compressing an input file
     * @param input
     * @param dictionarySize
     */
    public Dictionary(CompressionInput input, int numBits, int dictionarySize) {
        NUM_BITS = numBits;
        if (dictionarySize < 0 || dictionarySize > (int) Math.pow(2, NUM_BITS)) {
            throw new IllegalArgumentException("invalid dict size");
        }
        List <InstructionEntry> instructionEntries = sortInput(input);
        instructions = trimToSize(instructionEntries, dictionarySize);
    }

    private static List<InstructionEntry> sortInput(CompressionInput input) {
        Map<String, InstructionEntry> map = new HashMap<>();
        // get the count of all lines
        for(int i = 0; i!=input.size(); ++i) {
            final String line = input.getLine(i);
            InstructionEntry entry = map.get(line);
            if (entry == null) {
                // if the line is new, create a new IE and put it in.
                map.put(line, new InstructionEntry(line, i));
            } else {
                // otherwise, we just increment the count!
                entry.incrementCount();
            }
        }

        // sort the lines by their count
        List<InstructionEntry> instructionEntries = new ArrayList<>(map.values());
        Collections.sort(instructionEntries);
        return instructionEntries;
    }
    private static List<String> trimToSize(List<InstructionEntry> ieList, int size) {
        List<String> instructions = new ArrayList<>(size);
        for (int i=0; i!=size && i!=ieList.size(); ++i) {
            instructions.add(ieList.get(i).getInstruction());
        }
        return instructions;
    }

    public int size() {
        return instructions.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i!=size(); ++i) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(instructions.get(i));
        }
        return sb.toString();
    }
}

/**
 * CompressionInput is a utility class that represents a file that has
 *  been normalized into an array of lines.
 */
class CompressionInput implements Iterable<String> {

    private List<String> lines;
    public CompressionInput(String filename) throws IOException {
        lines = Files.readAllLines(Paths.get(filename));
        // if there is an extra line, let's remove it.
        if (lines.get(lines.size()-1).equals("")) {
            lines.remove(lines.size() - 1);
        }
    }

    public String getLine(int line) {
        return lines.get(line);
    }

    public int size() {
        return lines.size();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String> () {

            private final Iterator<String> iter = lines.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return iter.next();
            }
        };
    }
}

/**
 * DecompressionInput is a utility class that takes in a decompression output
 *  and automatically segments it by instruction.
 */
class DecompressionInput {
    private final List<String> dictList;
    private final List<String> compressedInstructions;

    public DecompressionInput(List<String> compressedLines, List<CompressionStrategy> strategies, String dictSep, int formatBits) {
        // now it's time to concat those lines into a single string
        StringBuilder giantString = new StringBuilder();
        // will represent the starting index of the dict index
        int dictIndex = 0;
        // run the concat loop until we hit the dictionary
        for (String line : compressedLines) {
            if (line.equals(dictSep)) {
                break;
            }
            giantString.append(line);
            ++dictIndex;
        }
        // go ahead and assign the dict since we are done with processing it
        // the dictionary starts one past the separator, which is why we add one.
        dictList = compressedLines.subList(dictIndex + 1, compressedLines.size());
        compressedInstructions = separateInstructions(strategies, giantString.toString(), formatBits);
    }

    public DecompressionInput(String filename, List<CompressionStrategy> strategies, String dictSep, int formatBits) throws IOException {
        this(Files.readAllLines(Paths.get(filename)), strategies, dictSep, formatBits);
    }

    public String getRawDict(int i) {
        return dictList.get(i);
    }

    public int rawDictSize() {
        return dictList.size();
    }

    public String getCmpdInstruction(int i) {
        return compressedInstructions.get(i);
    }
    public int cmpdInstrucionSize() {
        return compressedInstructions.size();
    }

    private static List<String> separateInstructions(List<CompressionStrategy> strategies, String compdText, int formatBits) {
        // List to hold separated compressed instructions.
        List<String> compdInstructs = new ArrayList<>();
        // holds current position in giganto string
        int currPos = 0;

        while (currPos + formatBits < compdText.length()) {
            // parse the bits (in binary) needed for this section
            String formatStr = compdText.substring(currPos, currPos + formatBits);
            final int format = Integer.parseInt(formatStr, 2);
            // disregard the format bits
            currPos += formatBits;
            // since the strategy orders are 1 to 1 with the format
            CompressionStrategy strategy = strategies.get(format);
            // get the length of the encoding so we can separate it properly
            final int encodingLen = strategy.getEncodingLength();
            // make sure that the encoding len is in bounds
            // (can happen due to padding)
            if (currPos + encodingLen < compdText.length()) {
                // since padding should only be zeros, there is something wrong if the format is
                // not.
                if (format != 0) {
                    throw new IllegalStateException("unexpected nonzero format during padding processing");
                }
                break;
            }
            String cmpdInstruction = compdText.substring(currPos, currPos + encodingLen);
            compdInstructs.add(cmpdInstruction);
            currPos += encodingLen;
        }
        return compdInstructs;
    }
}
