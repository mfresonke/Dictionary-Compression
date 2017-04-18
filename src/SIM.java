// Created by Maxwell Fresonke on 3/23/17
// MIT Licensed
// On my honor, I have neither given nor received unauthorized aid on this assignment

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SIM {
    /* Debug Settings */
    // TODO Change to false for turning in
    private static final boolean IS_TESTING = false;

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
    private static final int DICTIONARY_NUM_BITS = 4;
    private static final String DICTIONARY_SEPARATOR = "xxxx";

    private static final int BITMASK_BITMASK_SIZE = 4;
    private static final int BITMASK_STARTING_LOC_SIZE = 5;

    public static void main(String[] args) throws IOException {

        // Compression strategies in order of priority
        List<CompressionStrategy> compStrategies = Arrays.asList(
                new OriginalBinaryEncodingStrategy(),
                new RunLengthEncodingStrategy(),
                new BitmaskBasedEncodingStrategy(DICTIONARY_NUM_BITS, BITMASK_BITMASK_SIZE, BITMASK_STARTING_LOC_SIZE),   // Bitmask-Based Compression
                new ConsecMismatchStrategy(1),   // 1-bit Mismatch
                new ConsecMismatchStrategy(2),   // 2-bit consecutive mismatch
                new ConsecMismatchStrategy(4),   // 4-bit consecutive mismatch
                new TwoBitAnywhereMismatchStrategy(),   // 2-bit anywhere mismatch
                new DirectMatchEncodingStrategy(DICTIONARY_NUM_BITS)
        );

        if (IS_TESTING) {
            runTesting(compStrategies);
        } else {
            runProduction(compStrategies, args);
        }

    }

    private static void writeToFile(String filename, String output) throws FileNotFoundException {
        try(PrintStream ps = new PrintStream(filename)) { ps.println(output); }
    }

    private static void runProduction(List<CompressionStrategy> compStrategies, String[] args) throws IOException {
        // Parse Input Args
        if (args.length != 1) {
            System.err.println("Please enter the correct number of arguments");
            System.exit(2);
        }

        final String compOrDecompArg = args[0];
        String output = "";
        String outputFilename = "";

        if (compOrDecompArg.equals(FLAG_COMPRESS)) {
            CompressionInput compIn = new CompressionInput(FILENAME_COMPRESSION_INPUT);
            output = runCompression(compIn, compStrategies, false);
            outputFilename = FILENAME_COMPRESSION_OUTPUT;
        } else if (compOrDecompArg.equals(FLAG_DECOMPRESS)) {
            DecompressionInput decompIn = new DecompressionInput(FILENAME_DECOMPRESSION_INPUT, compStrategies, DICTIONARY_SEPARATOR, FORMAT_BITS);
            output = runDecompression(decompIn, compStrategies);
            outputFilename = FILENAME_DECOMPRESSION_OUTPUT;
        } else {
            System.err.println("Error, unrecognized argument '"+compOrDecompArg+"'.");
            System.exit(4);
        }

        writeToFile(outputFilename, output);
    }

    private static void runTesting(List<CompressionStrategy> compStrategies) throws IOException {

        CompressionInput compIn = new CompressionInput(FILENAME_COMPRESSION_INPUT);
        String compOut = runCompression(compIn, compStrategies, true);
        System.out.println("=================== COMPRESSION OUT ===================");
        System.out.println(compOut);
        System.out.println("=================== END COMPRESSION OUT ===================");
        writeToFile("max-compressed-sep.txt", compOut);
        writeToFile("max-compressed-smooshed.txt", runCompression(compIn, compStrategies, false));

        String[] compOutLines = compOut.split("\n");
        DecompressionInput decompIn = new DecompressionInput("max-compressed-smooshed.txt", compStrategies, DICTIONARY_SEPARATOR, FORMAT_BITS);
        String decompOut = runDecompression(decompIn, compStrategies);

        System.out.println("=================== DECOMPRESSION OUT ===================");
        System.out.println(decompOut);
        System.out.println("=================== END DECOMPRESSION OUT ===================");
        writeToFile("max-decompressed.txt", decompOut);
    }

    private static String runCompression(CompressionInput input, List<CompressionStrategy> compStrategies, boolean debugOut) throws IOException {

        CompressionOutputBuilder outputBuilder = new CompressionOutputBuilder(FORMAT_BITS, OUTPUT_WIDTH, debugOut);

        // Run a pass over time file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(input, DICTIONARY_NUM_BITS, DICTIONARY_SIZE);

        // run compressions
        // for each uncompressed input line...
        int currLine=0;
        while (currLine!=input.size()) {
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
                CompressionResult result = strategy.compress(dict, input, currLine);
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
            outputBuilder.add(bestMethodFormat, bestMethodOutput);
            // increment currLine by however many were consumed by the compression method.
            currLine += bestMethodLinesConsumed;
        }


        // create final output, inc. dictionary
        StringBuilder output = new StringBuilder();
        output.append(outputBuilder.toString());
        output.append("\n");
        output.append(DICTIONARY_SEPARATOR);
        output.append("\n");
        output.append(dict.toString());

        return output.toString();
    }

    private static String runDecompression(DecompressionInput input, List<CompressionStrategy> compStrategies) {
        DecompressionOutputBuilder output = new DecompressionOutputBuilder();

        // Run a pass over the input file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(input, DICTIONARY_NUM_BITS);

        int currLine = 0;
        while (currLine != input.cmpdInstrucionSize()) {
            String currLineStr = input.getCmpdInstruction(currLine);
            int format = input.getCmpdFormat(currLine);
            CompressionStrategy cs = compStrategies.get(format);
            cs.decompress(dict, output, currLineStr);

            ++currLine;
        }

        return output.toString();
    }

}


/* ======= Compression Strategies ======= */

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
    private static final int MAX_VAL = (int) (Math.pow(2, LEN_ENCODING) - 1);

    // used to ensure that RLE isn't used twice in a row!
    private String lastCompressed = "";

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {

        // if the index is zero, it means we can't possibly look back! Return null meaning "can't compress"
        if (inputToCompress == 0) {
            lastCompressed = "";
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
            lastCompressed = "";
            return null;
        }
        // to ensure that RLE isn't used twice in a row,
        //  we need to check if the last value that was compressed is equal to the current one.
        if (currLine.equals(lastCompressed)) {
            lastCompressed = "";
            return null;
        } else {
            lastCompressed = currLine;
        }
        // ok cool! We're almost there. Now for the fun stuff!
        // we need to look __ahead__ and see if any future lines
        //  can also be compressed. Isn't RLE fun?! :D
        int lookAheadCount = 0;
        int i = inputToCompress + 1;
        while (i < input.size() && lookAheadCount < MAX_VAL) {
            if (!input.getLine(i).equals(currLine)) {
                break;
            }
            ++lookAheadCount;
            ++i;
        }
        // linesConsumes = lookAheadCount + 1 since this we must count the current line, too.
        int linesConsumed = lookAheadCount + 1;
        String output = Formatter.genBinaryString(lookAheadCount, LEN_ENCODING);
        return new CompressionResult(output, linesConsumed);
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String lineToDecompress) {
        // parse the number of times to repeat (+1 due to zero indexing)
        final int timesToRepeat = Integer.parseInt(lineToDecompress, 2) + 1;
        // indexOf prev val to duplicate
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

class DirectMatchEncodingStrategy implements CompressionStrategy {

    private final int DICT_NUM_BITS;

    public DirectMatchEncodingStrategy(int dictNumBits) {
        DICT_NUM_BITS = dictNumBits;
    }

    @Override
    public int getEncodingLength() {
        return DICT_NUM_BITS;
    }

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        int dictIndex = dict.indexOf(input.getLine(inputToCompress));
        if (dictIndex == -1) {
            return null;
        }
        String output = Formatter.genBinaryString(dictIndex, DICT_NUM_BITS);
        return new CompressionResult(output);
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String lineToDecompress) {
        outputBuilder.add(dict.getFromBinaryString(lineToDecompress));
    }
}

class BitmaskBasedEncodingStrategy implements CompressionStrategy {

    private final int DICT_INDEX_SIZE;
    private final int BITMASK_SIZE;
    private final int STARTING_LOC_SIZE;

    private final int DICT_INDEX_MAX_VAL;
    private final int BITMASK_MAX_VAL;
    private final int STARTING_LOC_MAX_VAL;

    public BitmaskBasedEncodingStrategy(int dictNumBits, int bitmaskSize, int startingLocSize) {
        this.DICT_INDEX_SIZE = dictNumBits;
        this.BITMASK_SIZE = bitmaskSize;
        this.STARTING_LOC_SIZE = startingLocSize;

        DICT_INDEX_MAX_VAL = maxSizeForBin(DICT_INDEX_SIZE);
        BITMASK_MAX_VAL = maxSizeForBin(BITMASK_SIZE);
        STARTING_LOC_MAX_VAL = maxSizeForBin(STARTING_LOC_SIZE);
    }

    private static int maxSizeForBin(int numBits) {
        return (int) Math.pow(2, numBits) - 1;
    }

    @Override
    public int getEncodingLength() {
        return DICT_INDEX_SIZE + BITMASK_SIZE + STARTING_LOC_SIZE;
    }

    private String applyBitmask(String dictStr, int bitmask, int location) {
        final String bitMaskStr = Formatter.genBinaryString(bitmask, BITMASK_SIZE);
        // perform manipulation
        char dictChars[] = dictStr.toCharArray();
        char bitmaskChars[] = bitMaskStr.toCharArray();
        for (int bitmaskI = 0; bitmaskI != BITMASK_SIZE; ++bitmaskI) {
            final int dictI = bitmaskI + location;
            // if the bitmask says to flip...
            if (bitmaskChars[bitmaskI] == '1') {
                // flip the bit in the opposite of what it currently is.
                if (dictChars[dictI] == '1') {
                    dictChars[dictI] = '0';
                } else if (dictChars[dictI] == '0') {
                    dictChars[dictI] = '1';
                } else {
                    throw new IllegalStateException("somehow we're dealing with more than ones and zeros");
                }
            }
        }
        // now let's check to see if the flip is a match
        return new String(dictChars);
    }

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        String toCompress = input.getLine(inputToCompress);
        // let's brute force this bi'!
        // for every dictionary entry...
        for(int dictEntryI = 0; dictEntryI != dict.size(); ++dictEntryI) {
            String dictStr = dict.get(dictEntryI);
            // for every bitmask... (starting from the top to ensure that 1 is always at its leftmost position)
            for (int bitmask = BITMASK_MAX_VAL; bitmask != 0; --bitmask) {
                // for every possible starting location... (making sure to account for ones past the string)
                for(int location = 0; location < (dictStr.length() - BITMASK_SIZE); ++location) {
                    String flippedEntry = applyBitmask(dictStr, bitmask, location);
                    if (flippedEntry.equals(toCompress)) {
                        // WE'RE DONE!!!! WOOOOOOO... ok not really yet...
                        // build up our string rep
                        String bitRep =
                                Formatter.genBinaryString(location, STARTING_LOC_SIZE) +
                                        Formatter.genBinaryString(bitmask, BITMASK_SIZE) +
                                        Formatter.genBinaryString(dictEntryI, DICT_INDEX_SIZE);

                        return new CompressionResult(bitRep);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String toDecomp) {
        int start = 0;
        String locationStr = toDecomp.substring(start, start+STARTING_LOC_SIZE);
        start += STARTING_LOC_SIZE;
        String bitmaskStr = toDecomp.substring(start, start+BITMASK_SIZE);
        start += BITMASK_SIZE;
        String dictIndexStr = toDecomp.substring(start, start+DICT_INDEX_SIZE);

        int location = Integer.parseInt(locationStr, 2);
        int bitmask = Integer.parseInt(bitmaskStr, 2);
        int dictI = Integer.parseInt(dictIndexStr, 2);
        String dictEntry = dict.get(dictI);
        outputBuilder.add(applyBitmask(dictEntry, bitmask, location));
    }
}

class ConsecMismatchStrategy implements CompressionStrategy {

    private final int NUM_MISMATCHES;
    private static final int LEN_LOC = 5;
    private static final int LEN_DICT = 4;

    public ConsecMismatchStrategy(int numMismatches) {
        this.NUM_MISMATCHES = numMismatches;
    }

    @Override
    public int getEncodingLength() {
        return 9;
    }

    private String applyMismatch(int mismatchStart, String dictEntryStr) {
        char[] dictEntry = dictEntryStr.toCharArray();
        for (int i=0; i!=NUM_MISMATCHES; ++i) {
            int currI = mismatchStart + i;
            if (dictEntry[currI] == '1') {
                dictEntry[currI] = '0';
            } else if (dictEntry[currI] == '0') {
                dictEntry[currI] = '1';
            } else {
                throw new IllegalStateException("somehow we're dealing with more than ones and zeros");
            }
        }
        return new String(dictEntry);
    }

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        // string which we are attempting to match
        final String toMatch = input.getLine(inputToCompress);
        // every dictionary entry
        for (int dictI = 0; dictI!=dict.size(); ++dictI) {
            String dictEntry = dict.get(dictI);
            // every mismatch loc
            for (int mismatchStart=0; mismatchStart<(dictEntry.length() - NUM_MISMATCHES); ++mismatchStart) {
                // apply mismatch
                String generated = applyMismatch(mismatchStart, dictEntry);
                if (toMatch.equals(generated)) {
                    // we found a match!
                    // build output
                    String locStr = Formatter.genBinaryString(mismatchStart, LEN_LOC);
                    String dictStr = Formatter.genBinaryString(dictI, LEN_DICT);
                    return new CompressionResult(locStr + dictStr);
                }
            }
        }
        return null;
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String toDecomp) {
        int start = 0;
        String locationStr = toDecomp.substring(start, start+LEN_LOC);
        start += LEN_LOC;
        String dictIndexStr = toDecomp.substring(start, start+LEN_DICT);
        int location = Integer.parseInt(locationStr, 2);
        int dictI = Integer.parseInt(dictIndexStr, 2);
        outputBuilder.add(applyMismatch(location, dict.get(dictI)));
    }
}

class TwoBitAnywhereMismatchStrategy implements CompressionStrategy {

    final static int LEN_MM = 5;
    final static int LEN_DICT_I = 4;

    @Override
    public int getEncodingLength() {
        return LEN_MM * 2 + LEN_DICT_I;
    }

    private static String applyMismatch(String orig, int mm1, int mm2) {
        char[] dictChars = orig.toCharArray();
        // flip the bit in the opposite of what it currently is.
        if (dictChars[mm1] == '1') {
            dictChars[mm1] = '0';
        } else if (dictChars[mm1] == '0') {
            dictChars[mm1] = '1';
        } else {
            throw new IllegalStateException("somehow we're dealing with more than ones and zeros");
        }
        // flip the bit in the opposite of what it currently is.
        if (dictChars[mm2] == '1') {
            dictChars[mm2] = '0';
        } else if (dictChars[mm2] == '0') {
            dictChars[mm2] = '1';
        } else {
            throw new IllegalStateException("somehow we're dealing with more than ones and zeros");
        }
        return new String(dictChars);
    }

    @Override
    public CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress) {
        final String toMatch = input.getLine(inputToCompress);
        // for every dictionary entry
        for(int dictEntryI = 0; dictEntryI != dict.size(); ++dictEntryI) {
            String dictEntry = dict.get(dictEntryI);
            // for every mm1
            for(int mm1=0; mm1<dictEntry.length(); ++mm1) {
                for(int mm2=0; mm2<dictEntry.length(); ++mm2) {
                    // if they are the same skip
                    if (mm1 == mm2) {
                        continue;
                    }
                    String attempt = applyMismatch(dictEntry, mm1, mm2);
                    if (attempt.equals(toMatch)) {
                        String mm1Str = Formatter.genBinaryString(mm1, LEN_MM);
                        String mm2Str = Formatter.genBinaryString(mm2, LEN_MM);
                        String dictIStr = Formatter.genBinaryString(dictEntryI, LEN_DICT_I);
                        return new CompressionResult(mm1Str + mm2Str + dictIStr);
                    }
                }
            }

        }
        return null;
    }

    @Override
    public void decompress(Dictionary dict, DecompressionOutputBuilder outputBuilder, String toDecomp) {
        int start = 0;
        String mm1Str = toDecomp.substring(start, start+LEN_MM);
        start += LEN_MM;
        String mm2Str = toDecomp.substring(start, start+LEN_MM);
        start += LEN_MM;
        String dictIndexStr = toDecomp.substring(start, start+LEN_DICT_I);

        int mm1 = Integer.parseInt(mm1Str, 2);
        int mm2 = Integer.parseInt(mm2Str, 2);
        int dictI = Integer.parseInt(dictIndexStr, 2);
        String dictEntry = dict.get(dictI);
        outputBuilder.add(applyMismatch(dictEntry, mm1, mm2));
    }
}

/* ======= Helper Types ======= */

class Dictionary {
    private final int NUM_BITS;
    private final List<String> instructions;

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
        // indexOf the count of all lines
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

    /**
     * Get returns the specified instruction index.
     * @param instruction
     * @return index, or -1 if DNE
     */
    public int indexOf(String instruction) {
        return instructions.indexOf(instruction);
    }

    public String get(int i) {
        return instructions.get(i);
    }

    public String getFromBinaryString(String binaryString) {
        if (binaryString.length() != NUM_BITS) {
            throw new IllegalArgumentException("binaryString is of incorrect size");
        }
        int index = Integer.parseInt(binaryString, 2);
        return get(index);
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

class CompressionResult {
    private int linesConsumed;
    private String compressedLine;

    public CompressionResult(String compressedLine) {
        this(compressedLine, 1);
    }
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

/* ======= Input Helpers ======= */

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
    private List<String> compressedInstructions;
    private List<Integer> formatInts;

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
        separateInstructions(strategies, giantString.toString(), formatBits);
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

    public int getCmpdFormat(int i) {
        return formatInts.get(i);
    }

    public int cmpdInstrucionSize() {
        return compressedInstructions.size();
    }

    private void separateInstructions(List<CompressionStrategy> strategies, String compdText, int formatBits) {
        // List to hold separated compressed instructions.
        compressedInstructions = new ArrayList<>();
        formatInts = new ArrayList<>();
        // holds current position in giganto string
        int currPos = 0;

        while (currPos + formatBits < compdText.length()) {
            // parse the bits (in binary) needed for this section
            String formatStr = compdText.substring(currPos, currPos + formatBits);
            final int format = Integer.parseInt(formatStr, 2);
            currPos += formatBits;
            // since the strategy orders are 1 to 1 with the format
            CompressionStrategy strategy = strategies.get(format);
            // indexOf the length of the encoding so we can separate it properly
            // DEBUG
            if (strategy == null) {
                System.out.println("DEBUG");
            }
            // END DEBUG
            final int encodingLen = strategy.getEncodingLength();
            // make sure that the encoding len is in bounds
            // (can happen due to padding)
            if (currPos + encodingLen > compdText.length()) {
                // since padding should only be zeros, there is something wrong if the format is
                // not.
                if (format != 0) {
                    throw new IllegalStateException("unexpected nonzero format during padding processing");
                }
                break;
            }
            String cmpdInstruction = compdText.substring(currPos, currPos + encodingLen);
            compressedInstructions.add(cmpdInstruction);
            formatInts.add(format);
            currPos += encodingLen;
        }
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
                sb.append(temp.substring(index, Math.min(index + OUTPUT_WIDTH,temp.length())));
                sb.append("\n");
                index += OUTPUT_WIDTH;
            }
            // append some extra zeros!
            sb.deleteCharAt(sb.length()-1);
            while (sb.length()% (OUTPUT_WIDTH+1) != OUTPUT_WIDTH)
                sb.append("0");
            sb.append("\n");
        }
        // remove the extraneous newlines made in each loop
        sb.deleteCharAt(sb.length()-1);
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
        boolean first = true;
        for(String instruction : instructions) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }
            sb.append(instruction);
        }
        return sb.toString();
    }
}