
/**
 *  author: Abdeali Chandanwala
 *  
 *  To Do List: 
 *  table Metadata, 
 *  Plus Minus on Figures, 
 *  Proforma or not check,
 *  Units for the number e.g. dollars, shares
 *  Date associated with the number from the header eg 2001 on Table Level
 *  The date of the document from the index on Document Level metadata
 *  -An ability to specify sections of interest (NamedRecord)
 *  -Example Specified at the bottom is also quiet nice and appropriate 
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
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

	public ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
		// for (String html : args) {
		// Main main = new Main();
		// try {
		// main.parseHtml(html);
		// } catch (JsonProcessingException e) {
		// e.printStackTrace();
		// }
		// }

		Main main = new Main();

		String html = main.getFile("appleInc.html");
		try {
			main.parseHtml(html);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private String getFile(String fileName) {

		StringBuilder result = new StringBuilder("");

		// Get file from resources folder
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
		Document completeDocument = Jsoup.parse(html);

		Element document = completeDocument.getElementsByTag("text").first();

		// parse all the table required
		ObjectNode tableInfo = retrieveTableInfo(document);

		// get the metadata from the html
		ObjectNode metadataObject = retrieveMetadataInfo(completeDocument);
		tableInfo.set("Document Metadata", metadataObject);
		System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableInfo));
	}

	private ObjectNode retrieveMetadataInfo(Document document) {
		ObjectNode metaData = mapper.createObjectNode();
		String type = document.getElementsByTag("type").first().ownText();
		String companyName = findCompanyName(document);
		String employerIdentificationNo = findEmployerIdentificationNumber(document);
		metaData.put("Doc Type", type);
		metaData.put("Company Name", companyName);
		metaData.put("Employer Identification Number", employerIdentificationNo);
		return metaData;
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
					employerNo = element.getElementsMatchingText(getPattern()).eachText().get(0);
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

	private ObjectNode retrieveTableInfo(Element document) {
		Elements tables = document.getElementsByTag("table");
		ObjectNode allTableData = mapper.createObjectNode();
		tables.forEach(table -> {
			if (!isTableRequiredToBeParsedFurthur(table))
				return;

			String tableTitle;
			try {
				tableTitle = getTableTitle(table);
				System.out.println("Table Title: "+tableTitle);
			} catch (NullPointerException e) {
				return;
			}

			ArrayNode tableData = mapper.createArrayNode();
			Iterator<Element> rows = table.getElementsByTag("tr").iterator();
			int rowIndex = 0;
			int columnIndex = 0;
			String subHeader = "";
			ArrayList<String> tableHeader = new ArrayList<String>();
			while (rows.hasNext()) {
				Element row = (Element) rows.next();
				if (!row.text().trim().isEmpty()) {
					ObjectNode obj = mapper.createObjectNode();
					Iterator<Element> columns = row.getElementsByTag("td").iterator();
					if (!rowHasSubHeaders(row)) {
						columnIndex = 0;
						while (columns.hasNext()) {
							Element column = (Element) columns.next();
							if (!column.text().trim().isEmpty()) {
								String columnText = column.text();
								if (rowIndex == 0) {
									tableHeader.add(columnText);
									continue;
								}
								if (column.text().equals("$")) {
									continue;
								} else if (isNumeric(columnText)) {
									columnText.replaceAll(",", "");
								} else if (columnIndex == 0) {
									columnText = subHeader + columnText;
								}

								obj.put(tableHeader.get(columnIndex), columnText);
								columnIndex++;
							}
						}
						tableData.add(obj); //TODO: rectify issue
						rowIndex++;
					} else {
						subHeader += row.text() + "."; // adds the Sub Heading as per sequence
					}
				} else if (rowIndex > 0) {
					subHeader = "";
				}
			}

			if (tableTitle.isEmpty())
				return;
			try {
				System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableData));
			} catch (JsonProcessingException e) {
				System.out.println("UnRequired Error 1");
			}
			allTableData.set(tableTitle, tableData);
		});

		return allTableData;
	}

	private boolean isTableRequiredToBeParsedFurthur(Element table) {
		return !table.text().trim().isEmpty();
	}

	private boolean rowHasSubHeaders(Element row) {
		Iterator<Element> tds = row.getElementsByTag("td").iterator();
		int counter = 0;
		while (tds.hasNext()) {
			Element td = (Element) tds.next();
			if (!td.text().trim().isEmpty())
				counter++;
		}
		return counter == 1;
	}

	private String getTableTitle(Element table) throws NullPointerException {
		Iterator<Element> iterator = table.parents().iterator();
		String title;
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			if (element.tagName().equals("div")) {
				title = recurringPreviousSearch(element, 10);
				if (title.contains("table"))
					continue;
			}
		}
		return "table-" + new Random().nextLong();
	}

	private String recurringPreviousSearch(Element element, int iterateLimit) throws NullPointerException {
		String outerHtml = element.previousElementSibling().outerHtml();
		String title = Jsoup.parse(outerHtml).getElementsByAttributeValueContaining("style", "font-weight:bold").first()
				.ownText();
		if (title.isEmpty()) {
			if (iterateLimit > 0)
				return recurringPreviousSearch(element.previousElementSibling(), iterateLimit--);
			else
				return "table-" + new Random().nextLong();
		} else
			return title;
	}

	public static boolean isNumeric(String str) {
		return str.matches(",?\\d+(\\.\\d+)?"); // match a number with optional ',' and decimal.
	}
}
