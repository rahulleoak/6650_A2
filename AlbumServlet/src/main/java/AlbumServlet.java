import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@MultipartConfig
@WebServlet(name = "albumServlet", value = "/albums")
public class AlbumServlet extends HttpServlet {
  private static Map<String,Album> albumData= new HashMap<>();
  private static Map<String,String> imageMetadata = new HashMap<>();
  private final Gson gson = new Gson();
  private final DynamoDbClient dynamoDb;
  private final String tableName = "album"; // Change to your DynamoDB table name

  public AlbumServlet() {
    // Initialize DynamoDB client
    this.dynamoDb = DynamoDbClient.builder()
        .region(Region.US_WEST_2) // Change to your preferred region
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

  //FOR TESTING PURPOSES: start
  Album testAlbum = new Album("Sex Pistols","Never Mind The Bollocks","1977");
  //FOR TESTING PURPOSES: end
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("application/json");

    // Validate URL
    String urlPath = req.getPathInfo();
    if(urlPath == null || urlPath.isEmpty()){
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Extract albumID from URL
    String albumID = req.getPathInfo().substring(1);
    System.out.println(albumID);

    try {
      // Fetch album data from DynamoDB
      Map<String, AttributeValue> keyToGet = new HashMap<>();
      keyToGet.put("albumID", AttributeValue.builder().s(albumID).build());

      GetItemRequest getRequest = GetItemRequest.builder()
          .tableName(tableName)
          .key(keyToGet)
          .build();

      GetItemResponse getResult = dynamoDb.getItem(getRequest);

      if (getResult.item() == null || getResult.item().isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("Album not found");
        return;
      }

      // Assuming album data is stored in a "profile" attribute in DynamoDB
      String albumJson = getResult.item().get("profile").s();
      Album album = gson.fromJson(albumJson, Album.class);

      // Load JSON
      JsonObject albumJSON = new JsonObject();
      albumJSON.addProperty("albumID", albumID);
      albumJSON.addProperty("profile", gson.toJson(album));

      // Print response out
      PrintWriter out = resp.getWriter();
      out.print(gson.toJson(albumJSON));
      out.flush();

    } catch (DynamoDbException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");

    // Generate new albumID
    String albumID = UUID.randomUUID().toString();

    // Read profile part
    Part profilePart = req.getPart("profile");
    BufferedReader reader = new BufferedReader(new InputStreamReader(profilePart.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder profileContent = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null){
      profileContent.append(line);
    }
    String profileJSON = profileContent.toString();

    // Parse profileJSON to an album object
    Album newAlbum = gson.fromJson(profileJSON, Album.class);

    // Read image data as byte stream
    Part imagePart = req.getPart("image");
    float imageSize = 0;
    if (imagePart != null){
      imageSize = imagePart.getSize();
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.getWriter().write("Image not found");
      return;
    }

    try {
      // Store album data in DynamoDB
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("albumID", AttributeValue.builder().s(albumID).build());
      item.put("profile", AttributeValue.builder().s(profileJSON).build());
      item.put("imageSize", AttributeValue.builder().n(String.valueOf(imageSize)).build());

      PutItemRequest putRequest = PutItemRequest.builder()
          .tableName(tableName)
          .item(item)
          .build();

      dynamoDb.putItem(putRequest);

      // Create and send JSON response
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.addProperty("ID", albumID);
      jsonResponse.addProperty("imageSize", imageSize);
      PrintWriter out = resp.getWriter();
      out.print(gson.toJson(jsonResponse));
      out.flush();

    } catch (DynamoDbException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
