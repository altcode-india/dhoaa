
package ani.dhoaa;


import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * Licensed under AltCode GPv1 license.
 * http://www.altcode.in
 */
/**
 *
 * @author Aniruddha Sarkar
 */
public class YoutubeEngine {

    /**
     * Define a global variable that identifies the name of a file that contains
     * the developer's API key.
     */
    private static final long NUMBER_OF_VIDEOS_RETURNED = 15;

    /**
     * Define a global instance of a Youtube object, which will be used to make
     * YouTube Data API requests.
     */
    private static YouTube youtube;

    public YoutubeEngine() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            NodeList googleNode = dBuilder.parse(YoutubeEngine.class.getResourceAsStream("/xml/Auth.xml")).getElementsByTagName("google").item(0).getChildNodes();
            for (int i = 0; i < googleNode.getLength(); i++) {
                System.out.print("google->\t");
                Node youtubeN;
                if ((youtubeN = googleNode.item(i)).getNodeName().equals("youtube")) {
                    System.out.print(i+". Found: youtube->\t");
                    NodeList youtubeNode = youtubeN.getChildNodes();
                    for (int j = 0; j < youtubeNode.getLength(); j++) {
                        Node api_key;
                        if ((api_key = youtubeNode.item(j)).getNodeName().equals("apikey")) {
                            System.out.print(j+". Found: apikey\t");
                            apiKey = api_key.getTextContent();
                        } else {
                            System.out.print(j+". Not Found: apikey\t");
                        }
                    }
                } else {
                    System.out.print(i+". Not Found: youtube\t");
                }
                System.out.println();
            }
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), (HttpRequest hr) -> {
                //To change body of generated methods, choose Tools | Templates.
            }).setApplicationName("dhoaa").build();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(YoutubeEngine.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    String apiKey;

    public List<SearchResult> search(String query) throws IOException {
        YouTube.Search.List search = youtube.search().list("id");

        // Set your developer key from the {{ Google Cloud Console }} for
        // non-authenticated requests. See:
        // {{ https://cloud.google.com/console }}
        search.setKey(apiKey);
        search.setQ(query);

        // Restrict the search results to only include videos. See:
        // https://developers.google.com/youtube/v3/docs/search/list#type
        search.setType("video");

        // To increase efficiency, only retrieve the fields that the
        // application uses.
        search.setFields("items(id/kind,id/videoId)");
        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
        // Call the API and print results.
        SearchListResponse searchResponse = search.execute();
        //System.out.println(searchResponse.toPrettyString());
        return searchResponse.getItems();
    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query Search query (String)
     */
}
