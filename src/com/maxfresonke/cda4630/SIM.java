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

    public static void main(String[] args) {

        //DEBUG
        System.out.println("num args: " + args.length);
        //END DEBUG

        // Parse Input Args
        if (args.length != 1) {
            System.err.println("Please enter the correct number of arguments");
            System.exit(2);
        }

        String compOrDecompArg = args[0];

        try {
            if (compOrDecompArg.equals(FLAG_COMPRESS)) {
                runCompression(FILENAME_COMPRESSION_INPUT, FILENAME_COMPRESSION_OUTPUT);
            } else if (compOrDecompArg.equals(FLAG_DECOMPRESS)) {
                runDecompression(FILENAME_DECOMPRESSION_INPUT, FILENAME_DECOMPRESSION_OUTPUT);
            } else {
                System.err.println("Error, unrecognized argument '"+compOrDecompArg+"'.");
                System.exit(4);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static void runCompression(String input, String output) throws IOException {
        // Parse Input File
        InputFile inputFile = new InputFile(input);

        // Run a pass over time file counting the number of occurances of a given binary.
        Dictionary dict = new Dictionary(inputFile, SIZE_DICTIONARY);

        //DEBUG
        System.out.println(dict.toString());
        //END DEBUG


        // Get the top 12, preserving order.

        // other stuff ;)
    }

    private static void runDecompression(String input, String output) {

    }

}

class Dictionary {
    private static final int NUM_BITS = 4;
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
    public Dictionary(InputFile input, int dictionarySize) {
        if (dictionarySize < 0 || dictionarySize > 16) {
            throw new IllegalArgumentException("invalid dict size");
        }
        List <InstructionEntry> instructionEntries = sortInput(input);
        instructions = trimToSize(instructionEntries, dictionarySize);
    }

    private static List<InstructionEntry> sortInput(InputFile input) {
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
//            sb.append(generateBinaryIndex(i, NUM_BITS));
//            sb.append(" ");
            sb.append(instructions.get(i));
        }
        return sb.toString();
    }

    private static String generateBinaryIndex(int num, int numBits) {
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

/**
 * InputFile is a utility class that represents a file that has
 *  been normalized into an array of lines.
 */
class InputFile implements Iterable<String> {

    private List<String> lines;
    public InputFile(String filename) throws IOException {
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
