package org.ClientSide;

import com.google.gson.JsonParser;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;

public class RequestHandler {

  private final CloseableHttpClient httpClient;
  private final String baseUrl;

  //new add
  private final Gson gson = new Gson();
  private String lastAlbumID;
  //end add

  public RequestHandler(CloseableHttpClient httpClient, String baseUrl) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
  }

  public int sendGetRequest(String albumID) {
    int statusCode = -1;  // Initialize with error code
    try {
      String endPoint = baseUrl + "/albums/" + albumID;
      HttpGet httpGet = new HttpGet(endPoint);
      CloseableHttpResponse response = httpClient.execute(httpGet);

      try {
        statusCode = response.getStatusLine().getStatusCode();
      } finally {
        // Ensure the response is closed to free resources
        response.close();
      }
    } catch (IOException e) {
      e.printStackTrace(); // Log the exception
    }
    // Return the status code, which will be HTTP status or -1 in case of error
    return statusCode;

    //    try {
//      String endPoint = baseUrl + "/albums/" + albumID;
//      HttpGet httpGet = new HttpGet(endPoint);
//
//      HttpResponse response = httpClient.execute(httpGet);
//      int statusCode = response.getStatusLine().getStatusCode();
////      HttpEntity entity = response.getEntity();
////      String responseBody = EntityUtils.toString(entity);
////      System.out.println("GET Response Body: " + responseBody);
////      if (statusCode <= 200 && statusCode > 300) {
////        System.out.println("Error: " + statusCode + " - " + responseBody);
////      }
//      return statusCode;
//    } catch (IOException e) {
//      e.printStackTrace();
//      return -1;
//    }
  }

  public int sendPostRequest(String artist, String title, String year, File imageFile) {
    try {
      HttpClient client = this.httpClient;
      String endPoint = baseUrl + "/albums";
      HttpPost httpPost = new HttpPost(endPoint);

      String jsonProfileText = String.format("{\"artist\": \"%s\", \"title\": \"%s\", \"year\": \"%s\"}", artist, title, year);
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addBinaryBody("image", imageFile, ContentType.DEFAULT_BINARY, "nmtb.png");
      builder.addTextBody("profile", jsonProfileText, ContentType.APPLICATION_JSON);

      httpPost.setEntity(builder.build());
      HttpResponse response = client.execute(httpPost);
      int statusCode = response.getStatusLine().getStatusCode();
      //new add
      HttpEntity entity = response.getEntity();
      String responseBody = EntityUtils.toString(entity);
      JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
      if (responseJson.has("ID")) {
        lastAlbumID = responseJson.get("ID").getAsString();
      }
      EntityUtils.consume(entity);
      // end add

      return statusCode;


    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }

  public int handleResponse(HttpResponse response) throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      String responseBody = EntityUtils.toString(entity);
    }
    EntityUtils.consume(entity);
    return statusCode;
  }

  public String getLastAlbumID() {
    return lastAlbumID;
  }

}
