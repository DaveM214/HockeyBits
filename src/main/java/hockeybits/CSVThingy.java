package hockeybits;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class CSVThingy {

	final Map<String, String> teamsAndTheirLeagues = new HashMap<>();	
	final Map<String , List<String[]>> teamsAndTheirRecords = new HashMap<>();

	private final String teamsToLeagues = """
			West Women's Division 1 South,Ladies 1s,
			West Women's Wessex Division 1,Ladies 2s,
			AWest Women's Wessex Division 1,Ladies 3s,
			West Hockey League Men's Division 1 South,Mens 1s,
			West Men's Parrett Division 1,Mens 2s,
			West Men's Parrett Division 2,Mens 3s
			""";

	public CSVThingy()
	{
		
	}
	
	//Things to improve:
	//Input Robustness (sanitize "s)
	//Handling two teams in same leagues properly
	
	public int processCSV(String inputPath, String outputPath, boolean split)
	{
		// Populate the map.
		String temp = teamsToLeagues.replace("\n", "").replace("\r", "");
		String[] a = StringUtils.split(temp, ',');
		for (int i = 0; i < a.length; i = i + 2) {
			teamsAndTheirLeagues.put(a[i],a[i+1]);
			teamsAndTheirRecords.put(a[i+1], new ArrayList<>());
		}
		

		Reader in;
		try {		
			in = new FileReader(new File(inputPath).getAbsolutePath());
			
			@SuppressWarnings("resource")
			CSVParser parser = new CSVParser(in,  CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
			Iterable<CSVRecord> records = parser.getRecords();

			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);	
			
			// Get the bits we need out of the CSV
			for (CSVRecord record : records) {

				String date = record.get("Date");
				LocalDate dateTime = LocalDate.parse(date, inputFormatter);
				String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH));	
				String time = record.get("Time");
				String homeTeam = record.get("Home Team");
				String awayTeam = record.get("Away Team");
				
				//Need to strip venue out of 
				int venueIndex = StringUtils.indexOf(homeTeam,"(Venue:");

				String location = "";
				if(venueIndex >= 0)
				{
					//Remove all the crap surrounding the venue string.
					location = homeTeam.substring(venueIndex + 8, homeTeam.length() - 4).trim();
					homeTeam = homeTeam.substring(0,venueIndex);
				}
				
				//The last 7 characters are a random ID String so cut that out.
				String compEvent = record.get("Competition/Event");		
				String team = teamsAndTheirLeagues.get(compEvent.substring(0, compEvent.length() - 7));
				String[] entry = new String[]{formattedDate,time,homeTeam,awayTeam,location,team};
				teamsAndTheirRecords.get(team).add(entry);
			}

			List<Pair<String,List<String[]>>> printingList = new ArrayList<>();
			
			//If we are splitting input then put the values into their own lists.
			if(split)
			{
				for (Entry<String, List<String[]>> entry : teamsAndTheirRecords.entrySet()) 
				{
					printingList.add(new ImmutablePair<String, List<String[]>>("-"+entry.getKey(),entry.getValue()));
				}
			}
			else
			{	
				//Flatten into a single list otherwise
				printingList.add(new ImmutablePair<>("", teamsAndTheirRecords.entrySet()
						.stream()
						.flatMap(e -> e.getValue().stream())
						.collect(Collectors.toList()))); 
			}
			
			for (Pair<String, List<String[]>> pair : printingList) {
				
				String path = outputPath + pair.getLeft();
				if(!path.endsWith(".csv"))
					path  += ".csv";
				
				String out = new File(path).getAbsolutePath();
				BufferedWriter writer = Files.newBufferedWriter(Paths.get(out));	
				try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Date", "Time",
						"Home Team", "Away Team", "Location", "My Team Indicator"))) {
					csvPrinter.printRecords(pair.getRight());
					csvPrinter.flush();
					writer.close();
				}			
			}
			
			return 0;

		} catch (IOException e) {
			System.out.println("SOMETHING WENT HORRIBLY WRONG!!");
			e.printStackTrace();
			return 1;
		}
	}
	
}
