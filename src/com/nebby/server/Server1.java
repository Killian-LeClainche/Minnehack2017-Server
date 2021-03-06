package com.nebby.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nebby.server.network.Network;
import com.nebby.server.network.ServerNetwork;

public class Server1 
{
	private static final Logger log = LoggerFactory.getLogger(Server1.class);

	private static Server1 server;
	private static boolean stopServerThread = false;

	private Map<String, ServerNetwork> playerNetworks;
	private final ServerSocket listener;

	private Cipher cipher;
	private RSAPublicKey publicKey;
	private MongoClient client;
	private MongoCollection<Document> users;

	private int port;

	public static void main(String[] args) throws Exception
	{
		log.error("test");
		System.err.println("STARTING E!");
		System.out.println("STARTING!");

		int port = args.length > 0 ? Integer.parseInt(args[0]) : 8888;
		int size = args.length > 1 ? Integer.parseInt(args[1]) : 1024;

		server = new Server1(port, size);
	}

	public Server1(int port, int encryption) throws IOException
	{
		this.port = port;
		listener = new ServerSocket(port);
		playerNetworks = new HashMap<String, ServerNetwork>();
		if(encryption > 0)
		{
			try
			{
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(encryption);
				KeyPair key = keyGen.generateKeyPair();
				cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.DECRYPT_MODE, key.getPrivate());
				publicKey = (RSAPublicKey) key.getPublic();
			}
			catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e)
			{
				e.printStackTrace();
			}
		}

		new Thread()
		{
			@Override
			public void run()
			{
				Socket clientSocket;
				ServerNetwork newClient;
				while(!stopServerThread)
				{
					try 
					{
						clientSocket = listener.accept();
					} 
					catch (IOException e)
					{
						continue;
					}
					try 
					{
						log.error("TESTING!!!");
						System.err.println("CONNECTING ...");
						newClient = new ServerNetwork(server, clientSocket);
						if(playerNetworks.containsKey(newClient.getUUID()))
						{
							playerNetworks.get(newClient.getUUID()).invalidate();
							playerNetworks.remove(newClient.getUUID());
						}
						newClient.validate();
						playerNetworks.put(newClient.getUUID(), newClient);
						System.err.println("VALIDATED!");
					} 
					catch (IOException e)
					{
						continue;
					}
				}
				try 
				{
					listener.close();
					for(String uuid : playerNetworks.keySet())
					{
						playerNetworks.get(uuid).invalidate();
					}
				} 
				catch (IOException e)
				{
				}
			}
		}.start();
		//client = new MongoClient();
		//users = client.getDatabase("alexa").getCollection("users");
		update();
	}

	public void update()
	{
		new Thread()
		{
			public void run()
			{
				update();
			}

			public void update()
			{
				while(true)
				{
					for(String uuid : playerNetworks.keySet())
					{
						playerNetworks.get(uuid).update();
					}
					try
					{
						Thread.sleep(20);
					}
					catch (InterruptedException e) 
					{
						e.printStackTrace();
						return;
					}
				}
			}
		}.start();
	}

	public ServerNetwork getNetwork(UUID uuid)
	{
		return playerNetworks.get(uuid);
	}

	public int getServerPort()
	{
		return port;
	}

	public RSAPublicKey getPublicKey()
	{
		return publicKey;
	}

	public int getAESLength()
	{
		return 128;
	}

	public byte[] decryptRSA(byte[] encoded) 
	{
		try
		{
			return cipher.doFinal(encoded);
		}
		catch (IllegalBlockSizeException | BadPaddingException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void addPillToUser(Network network, String pillName, String pillDosage, String pillInterval) 
	{
		ServerNetwork serverNetwork = ((ServerNetwork) network);
		FindIterable<Document> iter = users.find(Filters.eq("id", serverNetwork.getUUID()));
		Document document;
		if(!iter.iterator().hasNext())
		{
			document = new Document();
			document.append("id", serverNetwork.getUUID());
			Document pillDoc = new Document();
			pillDoc.append("name", pillName);
			pillDoc.append("dosage", pillDosage);
			pillDoc.append("interval", pillInterval);
			document.append("pills", Arrays.asList(pillDoc));
			users.insertOne(document);
		}
		else
		{
			document = iter.first();
			Document newPill = new Document();
			newPill.append("name", pillName);
			newPill.append("dosage", pillDosage);
			newPill.append("interval", pillInterval);
			users.updateOne(Filters.eq("id", serverNetwork.getUUID()), new Document("$push", new Document("pills", newPill)));
		}
	}

	public void takePill(Network network, String name)
	{
		ServerNetwork serverNetwork = ((ServerNetwork)network);
		Document iter = users.find(Filters.eq("id", serverNetwork.getUUID())).first();
		
		Document pill = new Document();
		pill.append("name", name);
		
		
	}

	public void takeAllPills(Network network)
	{

	}

	public void pillsQuery(Network network)
	{

	}

}
