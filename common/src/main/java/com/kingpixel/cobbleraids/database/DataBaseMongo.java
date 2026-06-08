package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.UserInfo;
import com.kingpixel.cobbleraids.util.GsonCompat;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:53
 */
public class DataBaseMongo extends DataBaseClient {
  private MongoClient mongoClient;
  private MongoCollection<Document> raidCollection;
  private MongoCollection<Document> userInfoCollection;

  @Override
  public void connect() {
    CodecRegistry pojoCodecRegistry = fromRegistries(
      MongoClientSettings.getDefaultCodecRegistry(),
      fromProviders(PojoCodecProvider.builder().automatic(true).build())
    );

    var settings = MongoClientSettings.builder()
      .applicationName("CobbleRaids-MongoDB")
      .applyConnectionString(
        new ConnectionString(CobbleRaids.config.getDatabase().getUrl())
      )
      .uuidRepresentation(UuidRepresentation.STANDARD) // 👈 necesario para UUID
      .codecRegistry(pojoCodecRegistry)
      .build();

    mongoClient = MongoClients.create(settings);

    var database = mongoClient
      .getDatabase(CobbleRaids.config.getDatabase().getDatabase())
      .withCodecRegistry(pojoCodecRegistry); // ✅ apply codec to DB

    raidCollection = database.getCollection("raids");
    userInfoCollection = database.getCollection("user_info");

    CobbleRaids.LOGGER.info("Connected to MongoDB database");
  }

  @Override public void disconnect() {
    if (mongoClient != null) {
      mongoClient.close();
      CobbleRaids.LOGGER.info("MongoDB database disconnected");
    }
  }


  @Override public UserInfo findUserByPlayer(ServerPlayerEntity player) {
    var userInfo = cacheUser.getIfPresent(player.getUuid());
    if (userInfo != null) return userInfo;
    var document = userInfoCollection.find().filter(new Document("playerUUID",
      player.getUuid().toString())).first();
    if (document != null) {
      Object parsedUser = GsonCompat.fromJson(UtilsFile.getGson(), document.toJson(), UserInfo.class);
      if (parsedUser instanceof UserInfo user) {
        userInfo = user;
      }
    }
    if (userInfo != null) {
      cacheUser.put(player.getUuid(), userInfo);
      return userInfo;
    }
    userInfo = new UserInfo(player);
    saveOrUpdateUserInfo(userInfo);
    cacheUser.put(player.getUuid(), userInfo);
    return userInfo;
  }

  @Override public List<Raid> getHistoryRaids(int page, int pageSize) {
    var list = raidCollection.find()
      .skip((page - 1) * pageSize)
      .limit(pageSize)
      .into(new ArrayList<>());
    if (!list.isEmpty()) {
      var raids = new ArrayList<Raid>();
      for (var doc : list) {
        Object parsedRaid = GsonCompat.fromJson(UtilsFile.getGson(), doc.toJson(), Raid.class);
        if (parsedRaid instanceof Raid raid) {
          raids.add(raid);
        }
      }
      return raids;
    }
    return List.of();

  }

  @Override public void saveOrUpdateUserInfo(UserInfo userinfo) {
    userInfoCollection.replaceOne(
      new Document("playerUUID", userinfo.getPlayerUUID().toString()),
      Document.parse(UtilsFile.getGson().toJson(userinfo)),
      new ReplaceOptions().upsert(true)
    );
  }

  @Override public void saveOrUpdateHistoryRaid(Raid raid) {
    raidCollection.replaceOne(
      new Document("raidUUID", raid.getRaidUUID()),
      Document.parse(UtilsFile.getGson().toJson(raid)),
      new ReplaceOptions().upsert(true)
    );
  }
}
