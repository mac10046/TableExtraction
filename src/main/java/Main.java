
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main {

//	public ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
//		for (String html : args) {
//			Main main = new Main();
//			try {
//				main.parseHtml(html);
//			} catch (JsonProcessingException e) {
//				e.printStackTrace();
//			}
//		}
		
		Main main = new Main();
		
		String html = main.getFile("appleInc.html");
		try {
			main.parseHtml(html);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getFile(String fileName) {

		StringBuilder result = new StringBuilder("");

		//Get file from resources folder
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(fileName).getFile());

		try (Scanner scanner = new Scanner(file)) {

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result.append(line).append("\n");
			}

			scanner.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();

	  }

	public void parseHtml(String html) throws JsonProcessingException {
		Document document = Jsoup.parse(html);
		// parse all the table required
//		ArrayNode tableInfo = retrieveTableInfo(document);

		// get the metadata from the html
		ObjectNode metadataObject = retrieveMetadataInfo(document);
//		tableInfo.add(metadataObject);
//		System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableInfo));
	}

	@SuppressWarnings("unused")
	private ObjectNode retrieveMetadataInfo(Document document) {
		String type = document.getElementsByTag("type").text();
		String companyName = findCompanyName(document);
		String employerIdentificationNo = findEmployerIdentificationNumber(document);
		return null;
	}

	private String findEmployerIdentificationNumber(Document document) {
		String employerNo = "";
		employerNo = document.getElementsContainingText("I.R.S. Employer").prev("div").text();
		if (employerNo.isEmpty()) {
			Iterator<Element> iterator = document.getElementsContainingText("Employer Identification").prev("tr")
					.iterator();
			while (iterator.hasNext()) {
				Element element = (Element) iterator.next();
				if (element.is("tr")) {
					employerNo = element.getElementsMatchingText(getPattern()).text();
				}
			}
		}
		return null;
	}

	private Pattern getPattern() {
		String re1 = "(\\d+)";
		String re2 = "(-)";
		String re3 = "(\\d+)";

		return Pattern.compile(re1 + re2 + re3, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	}

	private String findCompanyName(Document document) {
		return document.getElementsContainingText("Exact name of Registrant").prev("div").text();
	}

	private ArrayNode retrieveTableInfo(Document document) {
		Elements tables = document.getElementsByTag("table");
		tables.forEach(table -> {
			if (Validator.isTableUsefull(table))
				return;
			String tableTitle = getTableTitle(document, table);

		});

		return null;
	}

	private String getTableTitle(Document document, Element table) {
		// TODO Auto-generated method stub
		return null;
	}
}
