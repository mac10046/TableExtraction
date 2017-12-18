import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main {
	public static void main(String[] args) {
		Main main = new Main();
		main.parseHtml(args[0]);
	}
	
	public void parseHtml(String html){
		Document document = Jsoup.parse(html);
		Elements tables = document.getElementsByTag("table");
		tables.forEach(tableElement -> {
			tableElement.previousSibling();
		});
	}
}
