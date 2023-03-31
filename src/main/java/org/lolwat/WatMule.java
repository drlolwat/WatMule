package org.lolwat;

import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.trade.Trade;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.randoms.LoginSolver;
import org.dreambot.api.randoms.RandomEvent;
import org.dreambot.api.randoms.RandomManager;
import org.dreambot.api.randoms.RandomSolver;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.script.listener.ExperienceListener;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.widgets.message.Message;
import org.dreambot.api.wrappers.widgets.message.MessageType;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ScriptManifest(name = "WatMule", description = "It is what it is", author = "lolwat",
        version = 1.0, category = Category.MISC, image = "")
public class WatMule extends AbstractScript implements ChatListener {

    private enum State {
        WAITING,
        LOGGED_IN,
        SETUP
    }

    private State currentState;
    private static List<String> queue;
    private int goldCollected = 0;
    private String target = "";

    private static void handleClient(Socket clientSocket) {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = clientSocket.getInputStream().read(buffer)) != -1) {
                String receivedMessage = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

                if(receivedMessage.startsWith("READY")) {
                    String[] sp = receivedMessage.split("\\|");
                    if(sp.length > 0) {
                        queue.add(sp[1]);
                        Logger.log("Added " + sp[1] + " to mule queue");
                        String response = "OK|" + Players.getLocal().getName();
                        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                        clientSocket.getOutputStream().write(responseBytes);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ServerSocket serverSocket;
    private ExecutorService executorService;

    @Override
    public void onMessage(Message m) {
        if(target.isEmpty()) {
            if (m.getType().equals(MessageType.TRADE)) {
                if (!queue.isEmpty() && queue.contains(m.getUsername())) {
                    target = m.getUsername();
                }
            }
        }
    }

    @Override
    public void onExit() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            Logger.log(e.toString());
        }
    }

    @Override
    public void onStart() {
        Client.getInstance().setMouseMovementAlgorithm(new BezierMouse());
        executorService = Executors.newFixedThreadPool(10);

        executorService.submit(() -> {
            try {
                serverSocket = new ServerSocket(8081);
                Logger.log("Mule server running");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException e) {
                Logger.log(e.toString());
            }
        });

        currentState = State.SETUP;
        queue = new ArrayList<>();
    }

    @Override
    public int onLoop() {
        switch(currentState) {
            case WAITING: {
                if(Client.isLoggedIn()) {
                    if(!Tabs.isOpen(Tab.LOGOUT)) {
                        Tabs.open(Tab.LOGOUT);
                        Tabs.logout();
                    }

                    getRandomManager().disableSolver(RandomEvent.LOGIN);
                    Logger.log("Disabled login solver");

                } else {
                    if(!queue.isEmpty()) {
                        getRandomManager().enableSolver(RandomEvent.LOGIN);
                        Logger.log("Enabled login solver");
                        currentState = State.LOGGED_IN;
                    }
                }

                return 5000;
            }

            case LOGGED_IN: {
                if(Client.isLoggedIn()) {
                    if(!queue.isEmpty()) {
                        if(target.isEmpty()) {
                            return 2000; // wait for target to trade me
                        }

                        if (Players.closest(target) != null && Players.closest(target).distance() <= 2) {
                            if (!Trade.isOpen()) {
                                Trade.tradeWithPlayer(target);
                                Sleep.sleepUntil(Trade::isOpen, 6000);
                                return 1;
                            } else {
                                if (Trade.isOpen(1)) {
                                    Trade.acceptTrade(1);
                                    Sleep.sleepUntil(() -> Trade.isOpen(2), 2000);
                                }

                                if (Trade.isOpen(2)) {
                                    Trade.acceptTrade(2);
                                    if(Trade.getItem(false, "Coins") != null) {
                                        goldCollected += Trade.getItem(false, "Coins").getAmount();
                                    }
                                    Sleep.sleepUntil(() -> !Trade.isOpen(), 2000);
                                    queue.remove(target);
                                    target = "";
                                    return 1;
                                }
                            }
                        }
                    } else {
                        currentState = State.WAITING;
                    }
                }
                return 3;
            }

            case SETUP: {
                if(NPCs.closest("Grand Exchange Clerk") != null) {
                    if(!Bank.isOpen()) {
                        Bank.open();
                        Sleep.sleepUntil(Bank::isOpen, 5000);

                        if(!Inventory.isEmpty()) {
                            Bank.depositAllItems();
                        }

                        Sleep.sleep(500, 750);

                        Bank.close();
                        currentState = State.WAITING;
                    }
                } else {
                    if(Walking.shouldWalk(6)) {
                        Walking.walk(BankLocation.GRAND_EXCHANGE);
                    }
                }
                return 3;
            }
            default:
                return 3;
        }
    }

    public static String simplifyNumber(double number) {
        if (number >= 1000000) {
            return String.format("%.2fM", number / 1000000);
        } else if (number >= 1000) {
            return String.format("%.2fK", number / 1000);
        } else {
            return String.format("%.2f", number);
        }
    }

    @Override
    public void onPaint(Graphics g) {
        g.setColor(new Color(204, 187, 154));
        g.fillRect(7,345, 506,130);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Verdana", Font.BOLD, 20));
        g.drawString("WatMule", 43, 377);

        g.setFont(new Font("Verdana", Font.BOLD, 15));
        g.drawString("GP collected: " + simplifyNumber(goldCollected), 43, 403);
    }
}