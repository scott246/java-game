/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * TODO:
 * adjust difficulty
 * add armor/shield feature
 * fix input lag
 * add more animation
 */
package game;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.Graphics;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Nathan
 */

@SuppressWarnings("serial")
public class Game extends JPanel {
    //time variables
    static long startTime = 0;
    static long pauseTime = 0;
    static long startPauseTime = 0;
    static long deathtime = 0;
    
    //enemy variables
    static int ENEMIES = 10000;//Math.ceil(Math.random() * 50);
    static Enemy[] enemyList = new Enemy[ENEMIES];
    
    //rock variables
    static int ROCKS = 100000;
    static Rock[] rockList = new Rock[ROCKS];
    
    //powerup variables
    static int POWERUPS = 200;
    static Powerup[] powerupList = new Powerup[POWERUPS];
    
    //store
    static Store store = new Store();
    
    //arrow (one at a time for now)
    static Arrow arrow;
    
    //screen dimensions
    static int xframe;
    static int yframe;
    static JFrame frame;
    static boolean backgroundDrawn = false;
    
    //bigger screen = more enemies
    static double screenSizeMultiplier;
    
    //player
    static Player p = new Player(); 
    
    //game variables
    static int aiTimer = 0;
    static boolean fog = false;
    static boolean terrain = false;
    static int level = 0;
    static int enemiesKilled = 0;
    static int enemiesRemaining = 0;
    static public AtomicBoolean paused;
    static public AtomicBoolean gameOver;
    static public Thread thread;
    static boolean instructionDisplay = false;
    static boolean playerIsDead = false;
    static String information = "";
    static String terminalCommand = "";
    static boolean noClip = false;

    public Game(int width, int height) throws IOException {
        xframe = width;
        yframe = height;
        paused = new AtomicBoolean(false);
        gameOver = new AtomicBoolean(false);
    }

    /**
     * Helper function to enforce a grid layout. Takes n and rounds it to the 
     * nearest size
     * @param n
     * @param size
     * @return
     */
    public static int roundLocation(int n, int size) {
        return (n + size-1) / size * size;
    }

    /**
     * Generates a map, border, and powerups
     * Algorithm: 
     *      generates a random power based on density
     *      takes that power and goes down the y axis until that power is reached
     *      places a rock, generates new power
     *      continue until you reach the bottom of the page, then increment x
     *      continue until you reach the bottom right edge of the screen
     */
    public static void generateMap() {
        int leftScreen = Rock.size * 7;
        int topScreen = Rock.size * 2;
        int rightScreen = xframe - Rock.size;
        int bottomScreen = yframe - Rock.size * 3;
        //start with a grid of filled cells and a cell list
        Rock[][] grid = new Rock[xframe/20 + 1][yframe/20 + 1];
        for (int a = 0; a < xframe; a += 20){
            for (int b = 0; b < yframe; b += 20){
                grid[a/20][b/20] = new Rock(a, b);
            }
        }
        
        ArrayList<Rock> cellList = new ArrayList<>();
        
        //mark middle cell as part of the maze
        grid[xframe/20/2][yframe/20/2] = null;
        
        //add surrounding filled cells to the cell list
        cellList.add(grid[xframe/20/2 - 1][yframe/20/2]);
        cellList.add(grid[xframe/20/2][yframe/20/2 - 1]);
        cellList.add(grid[xframe/20/2][yframe/20/2 + 1]);
        cellList.add(grid[xframe/20/2 + 1][yframe/20/2]);
        
        //while there are cells in the list
        while (!cellList.isEmpty()){
            //pick a random cell from the list
            int cellNumber = (int)(Math.random() * cellList.size());
            Rock r = cellList.get(cellNumber);
            //if cell doesn't have two explored neighbors
            int exploredNeighbors = 0;
            if (r.x + 20 < rightScreen && r.y + 20 < bottomScreen &&
                r.x - 1 > leftScreen && r.y - 1 > topScreen){
                if (grid[(r.x)/20 - 1][(r.y)/20] == null) exploredNeighbors++;
                if (grid[(r.x)/20][(r.y)/20 - 1] == null) exploredNeighbors++;
                if (grid[(r.x)/20][(r.y)/20 + 1] == null) exploredNeighbors++;
                if (grid[(r.x)/20 + 1][(r.y)/20] == null) exploredNeighbors++;
            }
            if (exploredNeighbors < 2 && r.x < xframe && r.y < yframe){
                //clear cell
                grid[(r.x)/20][(r.y)/20] = null;
                //add neighboring filled cells to cell list
                if (r.x + 20 < rightScreen && r.y + 20 < bottomScreen &&
                    r.x - 1 > leftScreen && r.y - 1 > topScreen){
                    if (!cellList.contains(grid[(r.x)/20 - 1][(r.y)/20]) && grid[(r.x)/20 - 1][(r.y)/20] != null) cellList.add(grid[(r.x)/20 - 1][(r.y)/20]);
                    if (!cellList.contains(grid[(r.x)/20][(r.y)/20 - 1]) && grid[(r.x)/20][(r.y)/20 - 1] != null) cellList.add(grid[(r.x)/20][(r.y)/20 - 1]);
                    if (!cellList.contains(grid[(r.x)/20][(r.y)/20 + 1]) && grid[(r.x)/20][(r.y)/20 + 1] != null) cellList.add(grid[(r.x)/20][(r.y)/20 + 1]);
                    if (!cellList.contains(grid[(r.x)/20 + 1][(r.y)/20]) && grid[(r.x)/20 + 1][(r.y)/20] != null) cellList.add(grid[(r.x)/20 + 1][(r.y)/20]);
                }
            }
                
            //remove cell from list
            cellList.remove(cellNumber);
        }
            
            
        //add filled cells to rockList
        int rockListPos = 0;
        ROCKS = 0;
        for (int c = 0; c < xframe/20; c++){
            for (int d = 0; d < yframe/20; d++){
                if (grid[c][d] != null){
                    rockList[rockListPos] = grid[c][d];
                    rockListPos++;
                    ROCKS++;
                }
            }
        }
        
        //control number of powerups spawned for larger screen sizes
        double sspm = 0; //sspm = screen size powerup multiplier
        switch(xframe){
            case 640: //640x480
                sspm = 1;
                break;
            case 800: //800x600
                sspm = .866;
                break;
            case 1024: //1024x720
                sspm = .707;
                break;
            case 1920: //1920x1080
                sspm = .5;
                break;
            default:
                sspm = .5;
                break;
        }

        //density of powerups (lower = less dense)
        double powerupFrequency = .03 * sspm;
        
        //generate a border with the rocks
        POWERUPS = 0;
        int rockCount = ROCKS;
        int powerupCount = 0;
        for (int a = 0; a < xframe; a+=Rock.size) {
            for (int b = 0; b < yframe; b+=Rock.size) {
                //use the rocks to generate a border
                if (
                        //left border
                        a == 0 || a == Rock.size || a == Rock.size * 2 ||
                        a == Rock.size * 3 || a == Rock.size * 4 || a == Rock.size * 5 ||
                        a == Rock.size * 6 || a == Rock.size * 7 ||
                        //top border
                        b == 0 || b == Rock.size || b == Rock.size * 2 ||
                        //right border
                        a == roundLocation(xframe, Rock.size)-Rock.size ||
                        a == roundLocation(xframe, Rock.size) ||
                        //bottom border
                        b == roundLocation(yframe, Rock.size)-Rock.size * 2 ||
                        b == roundLocation(yframe, Rock.size)-Rock.size * 3 ||
                        b == roundLocation(yframe, Rock.size)) {
                    rockList[rockCount] = new Rock();
                    ROCKS++;
                    rockList[rockCount].setX(roundLocation(a, Rock.size));
                    rockList[rockCount].setY(roundLocation(b, Rock.size)); 
                    rockCount++;
                }
                
                //if it's not a border, and not a rock location, there's a chance to put down a powerup
                else if (grid[a/20][b/20] == null) {
                    if (Math.random() < powerupFrequency) {
                            powerupList[powerupCount] = new Powerup();
                            powerupList[powerupCount].setX(roundLocation(
                                    a, Powerup.size));
                            powerupList[powerupCount].setY(roundLocation(
                                    b, Powerup.size));
                            powerupList[powerupCount].setType(Math.random());
                            powerupCount++;
                            POWERUPS++;
                    }
                }
            }
        }
    }
    
    /**
     * Used to calculate time alive for the HUD
     * @return time alive
     */
    public static double getTimeAlive(){
        if (gameOver.get()){
            return deathtime - startTime - pauseTime;
        }
        return System.currentTimeMillis() - startTime - pauseTime;
    }

    /**
     * Spawns bad guys randomly in the map
     */
    public static void generateEnemies() {
        //create the screen size multiplier
        switch (xframe){
            case 640: //640x480
                screenSizeMultiplier = 1;
                break;
            case 800: //800x600
                screenSizeMultiplier = 1.5;
                break;
            case 1280: //1280x720
                screenSizeMultiplier = 4;
                break;
            case 1920: //1920x1080
                screenSizeMultiplier = 9;
                break;
            default:
                screenSizeMultiplier = 16;
                break;
        }
        
        ENEMIES = (int)Math.ceil(10 * level * screenSizeMultiplier);
        for (int a = 0; a < ENEMIES; a++){            
            Enemy e = new Enemy();
            
            //generate a random x and y variable within the border
            int tempX = roundLocation(
                    (int) (Math.random() * (xframe - Rock.size * 8)) + 
                            Rock.size * 7,
                    e.getSize());
            int tempY = roundLocation(
                    (int) (Math.random() * (yframe - Rock.size * 5)) +
                            Rock.size * 3,
                    e.getSize());
            
            //ensure enemies don't spawn on player
            if (tempX == store.x && tempY == store.y) {
                tempX += e.getSize();
                tempY += e.getSize();
            }
            
            //ensure enemies don't spawn in rocks
            boolean enemyInRock = false;
            for (int b = 0; b < ROCKS; b++) {
                if (tempX == rockList[b].getX() &&
                        tempY == rockList[b].getY()){
                    ENEMIES--;
                    a--;
                    enemyInRock = true;
                    break;
                }
            }
            
            if (enemyInRock) continue;
            
            //set enemy properties
            e.setHP((int)Math.ceil((Math.random() * e.getMaxHP() * (level * .15))));
            e.setX(tempX);
            e.setY(tempY);
            e.setLastDir(0);
            enemyList[a] = e; 
            System.out.println("Enemy "+a+" created at ("+e.getX()+","+e.getY()+")");  
            
        }
    }

    /**
     * Basic collision detection system.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static boolean collisionDetect(int x1, int y1, int x2, int y2){
        return (x1 == x2 && y1 == y2);
    }

    /**
     * Initiates the basic AI system
     */
    public static void initAI(){
        if (!gameOver.get()){
            for (int a = 0; a < ENEMIES; a++){
                
                //uses a random number to decide which way the enemy goes
                double temp = Math.random();
                
                //go left
                if (temp < .25 && 
                        enemyList[a].getX()-enemyList[a].getSize() >= 0 &&
                        enemyList[a].getX()-enemyList[a].getSize() != Store.x) {
                    enemyList[a].setX(
                            enemyList[a].getX()-enemyList[a].getSize());
                    enemyList[a].setLastDir(2); //left
                }
                
                //go right
                else if (temp < .5 && temp >= .25 && 
                        enemyList[a].getX()+enemyList[a].getSize() < xframe &&
                        enemyList[a].getX()+enemyList[a].getSize() != Store.x) {
                    enemyList[a].setX(
                            enemyList[a].getX()+enemyList[a].getSize());
                    enemyList[a].setLastDir(4); //right
                }
                
                //go up
                if (temp < .75 && temp >= .5 && 
                        enemyList[a].getY()-enemyList[a].getSize() >= 0 &&
                        enemyList[a].getY()-enemyList[a].getSize() != Store.y) {
                    enemyList[a].setY(
                            enemyList[a].getY()-enemyList[a].getSize());
                    enemyList[a].setLastDir(1); //up
                }
                
                //go down
                else if (temp < 1 && temp >= .75 && 
                        enemyList[a].getY()+enemyList[a].getSize() < yframe &&
                        enemyList[a].getY()+enemyList[a].getSize() != Store.y) {
                    enemyList[a].setY(
                            enemyList[a].getY()+enemyList[a].getSize());
                    enemyList[a].setLastDir(3); //down
                }
            }
        }
    }
    
    /**
     * Changes the entire game screen to move on to the next level.
     * @param g
     * @param f
     * @throws IOException
     */
    public static void nextLevel(Game g, JFrame f) throws IOException {
        f.add(g);
        f.setSize(xframe, yframe);
        f.setResizable(false);
        f.setPreferredSize(new Dimension(xframe, Rock.size));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //change icon
        f.setIconImage(
                Toolkit.getDefaultToolkit().getImage(
                        JFrame.class.getResource("/images/logo.png")));
        f.setLocationRelativeTo(null);        
        f.setVisible(true);
        level++;
        aiTimer = 0;
        Trap.resetTraps();
        Player.resetPlayerLocation();
    }
    
    /**
     * Used to calculate which enemies will be damaged when attack magic
     * (destroying everything in a 3 block radius) is used.
     * @return enemies to be damaged
     */
    public static Enemy[] getEnemiesIn3BlockRadius() {
        Enemy[] enemies = new Enemy[50];
        int enemyCount = 0;
        for (int a = 0; a < ENEMIES; a++) {
            if (enemyList[a].getX() >= p.x - 3*p.size && 
                enemyList[a].getX() <= p.x + 3*p.size &&
                enemyList[a].getY() >= p.y - 3*p.size &&
                enemyList[a].getY() <= p.y + 3*p.size){
                enemies[enemyCount] = enemyList[a];
                enemyCount++;
            }
        }
        return enemies;
    }
    
    //used to calculate what the arrow will hit

    /**
     * Used to calculate which enemy will be damaged when an arrow is shot at 
     * it.
     * @param dir
     * @return enemy to be damaged
     */
    public static Enemy getEnemyInDirection(int dir) {
        int arrowx = p.x;
        int arrowy = p.y;
        boolean hitRock = false;
        while (!hitRock) {
            for (int a = 0; a < ROCKS; a++){
                if (rockList[a].getX() == arrowx &&
                    rockList[a].getY() == arrowy) {
                    hitRock = true;
                }
            }
            for (int b = 0; b < ENEMIES; b++) {
                if (enemyList[b].getX() == arrowx &&
                    enemyList[b].getY() == arrowy){
                    return enemyList[b];
                }
            }
            switch (dir) {
                case 1://up
                    arrowy-=p.size; break;
                case 2://left
                    arrowx-=p.size; break;
                case 3://down
                    arrowy+=p.size; break;
                case 4://right
                    arrowx+=p.size; break;
                default:
                    return null; //invalid input
            }
        }
        return null; //hit a rock or the end of the screen
    }

    /**
     * All the graphics for the game are 2D Graphics for now. This is the method
     * that draws them.
     * @param g 
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);  
        try {
            Paint.paint(g);
        } catch (IOException | URISyntaxException | FontFormatException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void gameLoop(boolean torchEnabled, boolean rocksEnabled) 
            throws InterruptedException, IOException {
        if (torchEnabled) {
            fog = true;
            System.out.println("TORCH MODE ON");
        }
        if (rocksEnabled){
            terrain = true;
            System.out.println("TERRAIN MODE ON");
        }
        p.x = roundLocation((int)xframe/2, p.size);
        p.y = roundLocation((int)yframe/2, p.size);
        store.x = p.x;
        store.y = p.y;
        startTime = System.currentTimeMillis();
        Database.dbConnect();
        Database.dbClean();
        frame = new JFrame("Underworld"); 
        
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            //KEY BINDINGS
            public void keyPressed(KeyEvent e) {
                try {
                    //go find the key binding
                    KeyBindings.bind(e.getKeyCode());
                } catch (FontFormatException | IOException ex) {
                    Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.setKeyCode(KeyEvent.KEY_RELEASED);
            }
        });
        Game game = new Game(xframe, yframe);
        nextLevel(game, frame);        
        generateMap();        
        generateEnemies();
        double enemyFPS = 2;
        final int gameFPS = 60;
        enemiesRemaining = ENEMIES;


        //MAIN GAME LOOP
        Runnable runnable = () -> {
            while (true) {
                
                //handle terminal input
                if (!terminalCommand.equals("")){
                    switch(terminalCommand){
                        case "kill":
                            p.hp = 0;
                            Database.addScore(enemiesKilled, level);
                            gameOver.set(true);
                            break;
                        case "stickyfingers":
                            p.gp += 100;
                            break;
                        case "revive":
                            p.hp += 100;
                            break;
                        case "quiver":
                            p.arrows += 100;
                            break;
                        case "wizard":
                            p.mana += 100;
                            break;
                        case "freebow":
                            p.bow = true;
                            break;
                        case "lildamage":
                             p.minDamage = p.maxDamage;
                            break;
                        case "massivedamage":
                            p.maxDamage += 20;
                            break;
                        case "saviour":
                            p.defenseMagic += 20;
                            p.mana = 200;
                            break;
                        case "angelofdeath":
                            p.attackMagic += 20;
                            p.mana = 200;
                            break;
                        case "trapqueen":
                            p.traps += 20;
                            break;
                        case "h4x0r":
                            p.gp += 100;
                            p.hp += 100;
                            p.mana += 100;
                            p.bow = true;
                            p.arrows += 100;
                            p.maxDamage += 20;
                            p.minDamage = p.maxDamage;
                            p.attackMagic += 20;
                            p.defenseMagic += 20;
                            p.traps += 20;
                            break;
                        case "noclip":
                            noClip = true;
                            break;
                        default:
                            break;
                    }
                    terminalCommand = "";
                }
                
                //player hp can't go over 100
                if (p.hp > 100) {
                    p.hp = 100;
                }
                
                //player mana can't be more than 100
                if (p.mana > 100) {
                    p.mana = 100;
                }
                
                //handle game pausing
                if (paused.get()) {
                    synchronized(thread) {
                        try {
                            frame.repaint();
                            thread.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Game.class.getName()).log(
                                    Level.SEVERE, null, ex);
                        }
                    }
                }
                if (gameOver.get()){
                    if (!playerIsDead){
                        deathtime = System.currentTimeMillis();
                    }
                    playerIsDead = true;
                }
                
                //animate stuff once per game loop
                if (Arrow.exists){
                    Arrow.animateArrowShot();
                }
                if (Magic.attackMagicExists){
                    Magic.animateAttackMagic();
                }
                if (Magic.defenseMagicExists){
                    Magic.animateDefenseMagic();
                }
                if (Error.errors){
                    Error.displayError(Error.activeError);
                    Error.errorDelay--;
                    if (Error.errorDelay == 0) {
                        Error.displayError(Errors.NOERROR);
                        Error.errorDelay = 60;
                    }
                }
                
                //calculate enemies killed
                int deadEnemies = 0;
                for (int a = 0; a < ENEMIES; a++) { 
                    if (enemyList[a].getHP() <= 0) {
                        deadEnemies++;
                    }
                }
                
                enemiesRemaining = ENEMIES - deadEnemies;
                
                //if all enemies are killed, make a new map
                if (ENEMIES == deadEnemies) {
                    frame.remove(frame);
                    Game g = null;
                    try {
                        g = new Game(xframe, yframe);
                    } catch (IOException ex) {
                        Logger.getLogger(Game.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                    try {
                        nextLevel(g, frame);
                    } catch (IOException ex) {
                        Logger.getLogger(Game.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                    generateMap();                    
                    generateEnemies();
                    enemiesRemaining = ENEMIES-enemiesKilled;
                }
                
                //move ai
                if (aiTimer >= (int)((100/enemyFPS)) / (level * .5)) {
                    aiTimer = 0;
                    initAI();
                }
                
                /* ==HANDLING COLLISIONS== */
                
                //if player collides with enemy, knock back player and calculate
                //  hp's/remove dead entities
                for (int a = 0; a < ENEMIES; a++){
                    if (collisionDetect(
                            p.x,
                            p.y,
                            enemyList[a].getX(),
                            enemyList[a].getY())){
                        p.hp -= Math.round(
                                Math.random() * enemyList[a].getHP());
                        enemyList[a].setHP(enemyList[a].getHP()-(int)Math.round(
                                p.minDamage + (
                                        Math.random() * (p.maxDamage-p.minDamage))));
                        if (enemyList[a].getHP() <= 0) {
                            Enemy.removeEnemy(a);
                            p.hp += Math.ceil(Math.random() * (10 * level));
                            p.gp += Math.ceil(Math.random() * level);
                        }
                        if (p.hp <= 0) {
                            Database.addScore(enemiesKilled, level);
                            gameOver.set(true);
                        }
                        p.knockbackPlayer();
                    }
                }
                
                //if player collides with powerup, remove the powerup and give 
                //  the player superpowers
                for (int a = 0; a < POWERUPS; a++) {
                    if (collisionDetect(
                            p.x, 
                            p.y, 
                            powerupList[a].getX(), 
                            powerupList[a].getY())) {
                        boolean much = false;
                        switch(powerupList[a].getType()) {
                            case GOLD:
                                p.gp += (int) Math.ceil(
                                        Math.random() * level * 10);
                                break;
                            case HEALTH:
                                if (p.hp < 100){
                                    p.hp += (int) Math.ceil(
                                        Math.random() * level * 10); 
                                }
                                else {
                                    Error.displayError(Errors.HIHP);
                                    much = true;
                                }
                                break;
                            case MINATTACK:
                                p.minDamage += (int) Math.ceil(
                                        Math.random() * level * 5);
                                if (p.minDamage > p.maxDamage){
                                    p.maxDamage = p.minDamage;
                                }
                                break;
                            case MAXATTACK:
                                p.maxDamage += (int) Math.ceil(
                                        Math.random() * level * 5);
                                break;
                            case MANA:
                                if (p.mana < 100){
                                    p.mana += (int) Math.ceil(
                                        Math.random() * level * 10);
                                }
                                else {
                                    Error.displayError(Errors.HIMANA);
                                    much = true;
                                }
                                break;
                        }
                        if (!much){
                            powerupList[a].setX(-Powerup.size);
                            powerupList[a].setY(-Powerup.size);
                        }
                        
                    }
                }
                
                //if player collides with rock, knock them back
                for (int a = 0; a < ROCKS; a++){
                    if (collisionDetect(
                            p.x, 
                            p.y, 
                            rockList[a].getX(), 
                            rockList[a].getY()) &&
                            !noClip){
                        p.knockbackPlayer();
                    }
                    
                    for (int b = 0; b < ENEMIES; b++){
                        //if enemy collides with rock, knock them back
                        if (collisionDetect(
                                enemyList[b].getX(), 
                                enemyList[b].getY(), 
                                rockList[a].getX(), 
                                rockList[a].getY())){
                            enemyList[b].knockbackEnemy();
                        }
                    }
                }
                
                for (int b = 0; b < ENEMIES; b++) {
                    //if enemy collides with enemy, knock an enemy back
                    for (int c = 0; c < ENEMIES; c++) {
                        if (collisionDetect(
                                enemyList[b].getX(), 
                                enemyList[b].getY(), 
                                enemyList[c].getX(), 
                                enemyList[c].getY()) && c != b){
                            enemyList[b].knockbackEnemy();
                        }
                    }

                    //enemy collides with powerup, knock enemy back
                    for (int d = 0; d < POWERUPS; d++) {
                        if (collisionDetect(
                                enemyList[b].getX(), 
                                enemyList[b].getY(), 
                                powerupList[d].getX(), 
                                powerupList[d].getY())){
                            enemyList[b].knockbackEnemy();
                        }
                    }
                    
                    //enemy collides with trap, spring it
                    for (int e = 0; e < Trap.MAXTRAPS; e++){
                        if (collisionDetect(
                                enemyList[b].getX(), 
                                enemyList[b].getY(), 
                                Trap.x[e], 
                                Trap.y[e])){
                            Trap.springTrap(enemyList[b]);
                            if (enemyList[b].getHP() <= 0) {
                                Enemy.removeEnemy(b);
                                p.hp += Math.ceil(Math.random() * (10 * level));
                                p.gp += Math.ceil(Math.random() * level);
                            }
                        }
                    }
                    
                    //enemy collides with store, knock enemy back
                    if (collisionDetect(
                            enemyList[b].getX(), 
                            enemyList[b].getY(), 
                            store.x, 
                            store.y)) {
                       enemyList[b].knockbackEnemy();
                    }
                }
                frame.repaint();
                try {
                    thread.sleep(1000/gameFPS);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Game.class.getName()).log(
                            Level.SEVERE, null, ex);
                }
                aiTimer++; 
            }
        };
        thread = new Thread(runnable);
        thread.start();
    }
}
