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
		System.out.println("Separate Output Files (Y/N)?:");
		boolean split = scanner.nextLine().equalsIgnoreCase("Y") ? true : false ;

		CSVThingy csvThingy = new CSVThingy();
		int status = csvThingy.processCSV(inputLocation, outputLocation, split);
		if (status > 0) {
			System.out.println("FAILURE!");
		} else {
			System.out.println("ALL DONE!");
		}
	}

}
