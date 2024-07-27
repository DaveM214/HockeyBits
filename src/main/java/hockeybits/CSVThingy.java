package hockeybits;

import java.io.BufferedReader;
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

	private final String club = "Yeovil & Sherborne";
	
	private final String teamsToLeagues = """
			West Women's Division 2 Central,Ladies 1s,
			West Women's Wessex Division 1,Ladies 2s,
			West Women's Wessex Division 2,Ladies 3s,
			West Men's Division 2 Central,Mens 1s,
			West Men's Parrett Division 1,Mens 2s,
			AWest Men's Parrett Division 1,Mens 3s
			""";

	public CSVThingy()
	{
		
	}
	
	//Things to improve:
	//Handling two teams in same leagues properly
	//Use a config file instead of hard coding
	
	@SuppressWarnings("resource")
	public int processCSV(String inputPath, String outputPath, boolean split, int mode)
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
			BufferedReader br = new BufferedReader(in);
			String line = null;
			List<String> lines = new ArrayList<>();
			// if no more lines the readLine() returns null
			while ((line = br.readLine()) != null) {
				// reading lines until the end of the file
				lines.add(line.replaceAll("\"", ""));
			}
						
			Iterable<CSVRecord> records = CSVParser.parse(lines.stream().collect(Collectors.joining(System.lineSeparator())),
					CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());

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
					//Remove all the crap surrounding the venue string. Offset is because a lot of venues end in (V) but not all of them.
					int locationOffset = homeTeam.contains("(V))") ? 4 : 1;
					location = homeTeam.substring(venueIndex + 8, homeTeam.length() - locationOffset).trim();
					homeTeam = homeTeam.substring(0,venueIndex);
				}
				
				//The last 7 characters are a random ID String so cut that out.
				String compEvent = record.get("Competition/Event").substring(0, record.get("Competition/Event").length() - 7);			
				String team = teamsAndTheirLeagues.get(compEvent);

				String homeAway = homeTeam.contains(club) ? "Home" : "Away";
				String oppo = homeAway.equals("Home") ? awayTeam : homeTeam;
				String[] entry = null;
				if (mode == 1) {
					entry = new String[] { formattedDate, time, homeTeam, awayTeam, location, team };
				} else if (mode == 2) {
					entry = new String[] { formattedDate, team, oppo, homeAway, compEvent, time, location };
				}
				else
				{
					throw new IOException("Use an actual mode");
				}
				teamsAndTheirRecords.get(team).add(entry);
			}

			List<Pair<String,List<String[]>>> printingList = new ArrayList<>();
			
			//If we are splitting input then put the values into their own lists.
			if(split)
			{
				teamsAndTheirRecords.entrySet().forEach(e -> printingList
						.add(Pair.of("-" + e.getKey(), e.getValue())));
			}
			else
			{	
				//Flatten into a single list 
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
				
				String[] header = null;
				if (mode == 1) {
					header = new String[] {"Date", "Time","Home Team", "Away Team", "Location", "My Team Indicator"};
				} else {
					header = new String[] {"Date", "My team", "Oppo", "Home-Away", "Competition", "Time", "Venue"};
				}
				
				try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header))) {
					csvPrinter.printRecords(pair.getRight());
					csvPrinter.flush();
					writer.close();
				}			
			}
			
			return 0;

		} catch (IOException e) {
			System.out.println("SOMETHING WENT HORRIBLY WRONG!!");
			System.out.println("Msg: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}
	
}
