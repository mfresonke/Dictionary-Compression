package com.maxfresonke.cda4630;


// Created by Maxwell Fresonke on 3/23/17
// MIT Licensed
// On my honor, I have neither given nor received unauthorized aid on this assignment

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SIM {

    /* Helper Constants */
    private static final String FILENAME_COMPRESSION_INPUT = "original.txt";
    private static final String FILENAME_COMPRESSION_OUTPUT = "cout.txt";
    private static final String FILENAME_DECOMPRESSION_INPUT = "compressed.txt";
    private static final String FILENAME_DECOMPRESSION_OUTPUT = "dout.txt";
    private static final String FLAG_COMPRESS = "1";
    private static final String FLAG_DECOMPRESS = "2";
    private static final int SIZE_DICTIONARY = 16;
    private static final String SEPARATOR_DICT = "xxxx";

    public static void main(String[] args) {

        // Parse Input Args
        if (args.length != 1) {
            System.err.println("Please enter the correct number of arguments");
            System.exit(2);
        }

        final String compOrDecompArg = args[0];
        String output = "";

        try {
            if (compOrDecompArg.equals(FLAG_COMPRESS)) {
                output = runCompression(FILENAME_COMPRESSION_INPUT, SIZE_DICTIONARY, SEPARATOR_DICT);
            } else if (compOrDecompArg.equals(FLAG_DECOMPRESS)) {
                output = runDecompression(FILENAME_DECOMPRESSION_INPUT);
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
    }

    private static String runCompression(String inputFilename, int dictSize, String dictSeparator) throws IOException {
        StringBuilder output = new StringBuilder();

        // Parse Input File
        CompressionInput inputFile = new CompressionInput(inputFilename);

        // Run a pass over time file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(inputFile, dictSize);

        // run other compressions

        // print out dictionary
        output.append(dictSeparator);
        output.append("\n");
        output.append(dict.toString());

        return output.toString();
    }

    private static String runDecompression(String input) {
        return null;
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
            throw new IllegalStateException("num greater than number bits");
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

class CompressionOutput {
    List<String> uncompressedInstructions = new LinkedList<>();
    List<String> compressedInstructions = new LinkedList<>();
    public void add(String uncompressed, String compressed) {

    }
}

class CompressionResult {
    private int linesConsumed;
    private String compressedLine;

    public CompressionResult(String compressedLine, int linesConsumed) {
        this.linesConsumed = linesConsumed;
        this.compressedLine = compressedLine;
    }

    public int getBitsUsed() {
        return compressedLine.length();
    }
    public int getLinesConsumed() {
        return linesConsumed;
    }
    public String getCompressedLine() {
        return compressedLine;
    }
}

interface CompressionStrategy {
    CompressionResult compress(Dictionary dict, CompressionInput input, int inputToCompress);
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
}


/* Helper Types */

class Dictionary {
    private static final int NUM_BITS;
    List<String> instructions;

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
     * Constructor for
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
