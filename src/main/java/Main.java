
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
		
//		stream().limit(100).forEach(elem -> {
//			int counter = 0;
//			System.out.println((counter++) + " ** " + elem.outerHtml());
//		});
//		System.exit(0);
		// parse all the table required
		ArrayNode tableInfo = retrieveTableInfo(document);

		// get the metadata from the html
		ObjectNode metadataObject = retrieveMetadataInfo(completeDocument);
		// tableInfo.add(metadataObject);
		// System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableInfo));
	}

	@SuppressWarnings("unused")
	private ObjectNode retrieveMetadataInfo(Document document) {
		String type = document.getElementsByTag("type").first().ownText();
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

	private ArrayNode retrieveTableInfo(Element document) {
		Elements tables = document.getElementsByTag("table");
		tables.forEach(table -> {
			if (Validator.isTableUsefull(table))
				return;

			ArrayNode tableData = mapper.createArrayNode();
			Iterator<Element> rows = table.getElementsByTag("tr").iterator();
			int rowIndex = 0;
			int columnIndex = 0;
			String subHeader = "";
			ArrayList<String> rowHeaders = new ArrayList<String>();
			while (rows.hasNext()) {
				Element row = (Element) rows.next();
				if (row.hasText()) {
					Iterator<Element> columns = row.getElementsByTag("td").iterator();
					if (!rowHasSubHeaders(row)) {
						while (columns.hasNext()) {
							Element column = (Element) columns.next();
							if (column.hasText()) {
								if (rowIndex == 0) {
									rowHeaders.add(column.text());
									continue;
								}
								if (column.text().equals("$"))
									continue;
								ObjectNode obj = mapper.createObjectNode();
								obj.put(rowHeaders.get(columnIndex), subHeader + column.text());
								tableData.add(obj);
								columnIndex++;
							}
						}
					} else {
						subHeader += row.text() + ".";
					}
					rowIndex++;
				} else if (rowIndex > 0) {
					subHeader = "";
				}
			}

			String tableTitle = getTableTitle(table);

		});

		return null;
	}

	private boolean rowHasSubHeaders(Element row) {
		Iterator<Element> tds = row.getElementsByTag("td").iterator();
		int counter = 0;
		while (tds.hasNext()) {
			Element td = (Element) tds.next();
			if (td.hasText())
				counter++;
		}
		return counter > 1;
	}

	private String getTableTitle(Element table) {
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

	private String recurringPreviousSearch(Element element, int iterateLimit) {
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
}
