# EventRecommender
 A personalization-based event recommender using TicketMaster API & user behavior analysis of our system using ELK on Amazon EC2
## Business Design
- **Why**: Many events most people like but you may not like, or most people don't know but you may like, so we need a 
personalization based recommendation system for event search.
- Use Case:
  * **Search** nearby event
  * Set event as **favorite**
  * Get **recommended** event 
## Overview of project
- 3 - tier architecture
  * Presentation tier: HTML5, CSS, JavaScript, Jquery Ajax.
  * Logic tier: Java
  * Data tier: MySQL, MongoDB
![image](https://user-images.githubusercontent.com/38120488/38473675-087633c6-3b62-11e8-8901-96afffa2c78f.png)
- Recommendation algorithm
  * Using **content-based recommendation** as **cold start**. Find categories of user's favorite events, and recommend similar events from TicketMaster API with similar categories.
- **Benchmark**
  * Handle about **150 QPS**(query per second) tested by **Apache JMeter** on EC2.
  
## API design
- Search
- favorite
  * get favorite
  * set favorite
  * unset favorite
- recommendation

![image](https://user-images.githubusercontent.com/38120488/38473945-be2e55a6-3b65-11e8-8358-011f267195da.png)

## DB design
- MySQL
  * **Item** - Event info
  * **User** - User info
  * **category** - item-category relationship
  * **history** - favorite history
  ![image](https://user-images.githubusercontent.com/38120488/38480030-08dbcca2-3b91-11e8-8c90-184f7e818758.png)

- MongoDB
  * collections( MongoDB collection == MySQL tables) 
    * **users** - store user information and favorite history. 
    * **items** - store information and item-category relationship.
    * **logs** - used in user behavior analysis to find peak time QPS of the system.
  * CRUD operations, please see [doc](https://docs.mongodb.com/manual/crud/)

## Implementation Details
- TicketMaster API
  * [doc - DISCOVERY API](https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/)
  * example: Get nearby 50 miles music related events 
    > `https://app.ticketmaster.com/discovery/v2/events.json?apikey=12345&geoPoint=abcd&keyword=music&radius=50`
- Geohash Encoding and Decoding Algorithm
  * since TicketMaster asked to use Geohash instead of latitude and longtitude directly in request, I used algorithm [here](https://developer-should-know.com/post/87283491372/geohash-encoding-and-decoding-algorithm) to convert encode/decode (latitude,longtitude) pair to Geohash.
- CORS issue
  * to fix CORS issue, please take look at doc [here](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#Preflighted_requests). Need figure out it's a **simple request** or a **Preflighted requests**, and need add something like 
  ```java
    response.setContentType("application/json");
    response.addHeader("Access-Control-Allow-Origin", "*");  
  ```
  on server side to respond to client.
  
## User behavior analysis
  * use **ElasticSearch** stores all traffic logs of the system.
  * use **Logstash**(data processing pipeline) to realtime monitor request and log changes, filter results and save to ElasticSearch.
    * please check logstash pipeline file [**logstash_pipeline.conf**](./logstash_pipeline.conf). 
    ![image](https://user-images.githubusercontent.com/38120488/38480242-651a17f2-3b92-11e8-9658-8da3b5a69fb2.png)
  * use **Kibana** to visualize ElasticSearch data.
    * Display where do users use our system
    ![image](https://user-images.githubusercontent.com/38120488/38480048-2f351ebc-3b91-11e8-9bc7-d0cf30effe3b.png)
  * **Offline log analysis to find peak time** using MongoDB MapReduce
    * One GET favorite request example in log to be analyzed.
    ```
    73.223.210.212 - - [19/Aug/2017:22:00:24 +0000] "GET /EventRecommender/history?user_id=1111 HTTP/1.1" 200 11410
    ```

    * See [**Purify.java**](./src/offline/Purify.java) to parse tomcat_logs and save to mongoDB, [**FindPeak.java**](./src/offline/FindPeak.java) to do MapReduce jobs. See pseudo code of MapReduce below:
    ```
      function map(String url, String time):
        If request url starts with /Titan:
        emit (time, 1)

      function reduce(Iterable<Integer> values):
        return Array.sum(values)
    ```
    See a sample tested result:

    ![image](https://user-images.githubusercontent.com/38120488/38509866-27e94506-3bf1-11e8-96f4-5c76089bc06e.png)



  
