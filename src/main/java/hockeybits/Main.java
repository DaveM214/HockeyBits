package hockeybits;

import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter Path to input CSV:");
		String inputLocation = scanner.nextLine();
		System.out.println("Enter Path spit the output to:");
		String outputLocation = scanner.nextLine();

		CSVThingy csvThingy = new CSVThingy();
		int status = csvThingy.processCSV(inputLocation, outputLocation);
		if (status > 0) {
			System.out.println("FAILURE!");
		} else {
			System.out.println("ALL DONE!");
		}
	}

}
