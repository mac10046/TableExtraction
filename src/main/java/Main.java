import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main {

	public static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
		for (String html : args) {
			Main main = new Main();
			try {
				main.parseHtml(html);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}

	public void parseHtml(String html) throws JsonProcessingException {
		Document document = Jsoup.parse(html);
		// parse all the table required
		ArrayNode tableInfo = retrieveTableInfo(document);

		// get the metadata from the html
		ObjectNode metadataObject = retrieveMetadataInfo(document);
		tableInfo.add(metadataObject);
		System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableInfo));
	}

	private ObjectNode retrieveMetadataInfo(Document document) {
		// TODO Auto-generated method stub
		return null;
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
