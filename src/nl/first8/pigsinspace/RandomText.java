package nl.first8.pigsinspace;


import java.util.Random;

public class RandomText {
	private static final int MAX_LENGTH = 32000;
	private static final int MIN_LENGTH = 25000;

	private static final int MIN_WORDS_IN_SENTENCE = 5;
	private static final int MAX_WORDS_IN_SENTENCE = 50;

	private static final int MIN_LETTERS_IN_WORD = 1;
	private static final int MAX_LETTERS_IN_WORD = 20;

	private static final Random RANDOM = new Random();

	private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			String text = randomText();
			System.out.println(text);
			System.out.println("length = " + text.length());
		}
	}

	public static String randomText() {
		StringBuilder sb = new StringBuilder();

		while (sb.length() < MIN_LENGTH) {
			sb.append(randomSentence());
		}

		if (sb.length() > MAX_LENGTH) {
			sb.substring(0, MAX_LENGTH - 1);
			sb.append(".");
		}
		return sb.toString();
	}

	public static StringBuilder randomSentence() {
		int nrOfWords = MIN_WORDS_IN_SENTENCE + RANDOM.nextInt(MAX_WORDS_IN_SENTENCE - MIN_WORDS_IN_SENTENCE);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nrOfWords; i++) {
			sb.append(randomWord(i == 0));
			if (i != nrOfWords - 1)
				sb.append(" ");
		}
		sb.append(". ");
		return sb;
	}

	public static StringBuilder randomWord(boolean capitalize) {
		int nrOfCharacters = MIN_LETTERS_IN_WORD + RANDOM.nextInt(MAX_LETTERS_IN_WORD - MIN_LETTERS_IN_WORD);
		return randomWord(capitalize, nrOfCharacters);
	}

	public static StringBuilder randomWord(boolean capitalize, int nrOfCharacters) {
		StringBuilder sb = new StringBuilder();

		int start = 0;
		if (capitalize) {
			start = 1;
			sb.append(Character.toUpperCase(randomCharacter()));
		}
		for (int i = start; i < nrOfCharacters; i++) {
			sb.append(randomCharacter());
		}

		return sb;
	}

	public static char randomCharacter() {
		int index = RANDOM.nextInt(CHARACTERS.length());
		return CHARACTERS.charAt(index);
	}

}
