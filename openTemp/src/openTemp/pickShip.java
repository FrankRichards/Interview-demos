/**
 * 
 */
package openTemp;

import java.util.HashMap;
import java.io.*;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.ArrayList;
/**
 * @author frank
 *
 */
public class pickShip {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Path to Inventory file
		Inventory current = new Inventory(args[0]); 
		//path to order file
		Order thisOrder = new Order(args[1], current);
		Shipment thisShip = thisOrder.shipIt();
		thisShip.toFile();
	}

}
class Inventory {
	private HashMap<String, Double> prodList;

	public Inventory(String filename) {
		prodList = new HashMap<String, Double>();
		String lineIn, codeIn, weightIn;
		BufferedReader inFile;
		try {
			inFile = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("Inventory file not found");
			return;
		}
		try {
			if (!inFile.readLine().equals("INVENTORY START")) {
				System.out.println("Inventory file doesn't have a valid header");
				inFile.close();
				return;}
			lineIn = inFile.readLine();
			while ((!lineIn.equals(null)) && (!lineIn.equals("INVENTORY END"))){
				if (!lineIn.equals("ITEM START")){
					System.out.println("Invalid Item start line" );
				}
				codeIn = inFile.readLine();
				if (!codeIn.startsWith("CODE:")){
					System.out.println("invalid code line");
					inFile.close();
					return;
				}
				inFile.readLine(); //throw name away, not used.
				weightIn = inFile.readLine();
				if(!weightIn.startsWith("WEIGHT:")){
					System.out.println("invalid code line");
					inFile.close();
					return;
				}
				//chop XXX: off the front
				prodList.put(codeIn.substring(6), new Double(weightIn.substring(8)));
				if (!(lineIn = inFile.readLine()).equals("ITEM END")){
					System.out.println("Invalid Item end line" );
				}
				lineIn = inFile.readLine();
			}
		} catch (IOException e1) {
			// As good as anything any, error here will be a weirdo.
			e1.printStackTrace();
		}

		try {
			inFile.close();
		} catch (IOException e) {
			// as above.
			e.printStackTrace();
			return;
		}
		return;
	}

	public Double getWeight (String prodCode) {
		return prodList.get(prodCode);
	}
}

class Order {
	private int orderNum;
	private Comparator<OrderItem> byWeight;
	private TreeSet<OrderItem> items;
	private Double totalWeight;

	public Order(String filename, Inventory curInv){
		String lineIn;
		BufferedReader ordFile;
		int numTemp;
		Double weightTemp;
		String[] entry;
		totalWeight = 0.0;
		byWeight = new Comparator<OrderItem>(){
			@Override
			public int compare(OrderItem I1, OrderItem I2) {
				return(Double.compare(I1.getWeight(), I2.getWeight()));
			}		
		};
		items = new TreeSet<OrderItem>(byWeight);
		try {
			ordFile = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("Order file not found");
			return;
		}
		try {
			if (!ordFile.readLine().equals("ORDER START")) {
				System.out.println("Order file doesn't have a valid header");
				ordFile.close();
				return;}
			lineIn = ordFile.readLine();
			if(!lineIn.startsWith("ORDER NUMBER:")){
				System.out.println("invalid order number line");
				ordFile.close();
				return;
			}	
			orderNum = Integer.parseInt(lineIn.substring(14));
			lineIn = ordFile.readLine(); //throw away customer code
			lineIn = ordFile.readLine();
			while ((!lineIn.equals(null)) && (!lineIn.equals("ORDER END"))){
				if (!lineIn.startsWith("ITEM: ")){
					System.out.println("Invalid Item line" );
				}
				entry = lineIn.split(" +");
				numTemp = Integer.parseInt(entry[2]);
				//Bogus. This is worse than left$() in Dartmouth Basic.
				entry[1] = entry[1].substring(0,entry[1].length() - 1);
				weightTemp = curInv.getWeight(entry[1]);
				items.add(new OrderItem(entry[1], numTemp, weightTemp));
				totalWeight += weightTemp * numTemp;
				lineIn = ordFile.readLine();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			// a decent default for a strange error.
			e1.printStackTrace();
		}

		try {
			ordFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		return;
	}
	public Shipment shipIt(){
		Shipment thisShip = new Shipment(orderNum, totalWeight);
		Box thisBox = new Box();
		OrderItem tempItem;
		Double boxWeight = 0.0;
		//weight of item
		Double tempWeight;
		while(!items.isEmpty()){
			//get the biggest item
			tempItem = items.last();
			items.remove(tempItem);
			tempWeight = tempItem.getWeight();
			//and put as many will fit in the box
			while((boxWeight < 10.0) && (tempItem != null)){
				while(((boxWeight + tempWeight) <= 10.0) && (tempItem.getQuantity() > 0)){
					thisBox.add(tempItem.getCode(),1,tempWeight);
					boxWeight += tempWeight;
					tempItem.decrementQuant();
				}
				//if another item won't fit, or all of this item are packed
				if(((boxWeight + tempWeight) >= 10.0) ||(tempItem.getQuantity() == 0)) {
					//if there are any left, put them back in the todo list.
					if (tempItem.getQuantity() > 0) {items.add(tempItem);}
					//and see if any smaller item will fit.
					tempItem = items.floor(new OrderItem("42",1,10.0 - boxWeight));
					//if so, get it. If not, we'll fall through at the while.
					if(tempItem != null){
					items.remove(tempItem);
					tempWeight = tempItem.getWeight();
					}
				}

			}
			thisShip.addBox(thisBox);
			thisBox = new Box();
			boxWeight = 0.0;
		}
		return(thisShip);
	}
}


class OrderItem {
	private String code;
	private int quantity;
	private Double weight;

	OrderItem(String inCode, int inQuant, Double inWeight){
		code = inCode;
		quantity = inQuant;
		weight = inWeight;
	}
	public Double getWeight(){
		return (this.weight);
	}
	public int getQuantity(){
		return(this.quantity);
	}
	public void addQuantity(int quant){
		this.quantity += quant;
	}
	public void decrementQuant(){
		this.quantity = this.quantity - 1;
	}
	public String getCode(){
		return(this.code);
	}
}

class Shipment {
	private ArrayList<Box> boxes;
	private int orderNum;
	private Double totalWeight;
	private int numBox;

	Shipment(int oNum, double weight){
		orderNum = oNum;
		totalWeight = weight;
		numBox = 0;
		boxes = new ArrayList<Box>();
	}

	public void addBox(Box thisBox){
		boxes.add(thisBox);
		numBox++;
	}

	public void toFile(){
		Box thisBox = new Box();
		try{
			BufferedWriter outFile = new BufferedWriter(new FileWriter("./ship" + orderNum + ".txt"));
			outFile.write("PICK SHIP START");
			outFile.newLine();
			outFile.write("ORDER NUMBER: " + orderNum);
			outFile.newLine();
			outFile.write("TOTAL SHIP WEIGHT: " + totalWeight);
			outFile.newLine();
			//boxes will be numbered backwards. Hope it's np.
			while(numBox > 0){
				outFile.write("BOX START: " + numBox);
				outFile.newLine();
				numBox--;
				thisBox = boxes.get(0);
				boxes.remove(0);
				outFile.write("SHIP WEIGHT: " + thisBox.getWeight());
				outFile.newLine();
				while(thisBox.getQuant() > 0){
					outFile.write("ITEM: " + thisBox.getCode() +", " + thisBox.getQuant());
					outFile.newLine();
					thisBox.nukeFirst();
				}
				outFile.write("BOX END");
				outFile.newLine();
			}
			outFile.write("PICK SHIP END");
			outFile.newLine();
			outFile.flush();
			outFile.close();
			return;
		}
		catch(IOException e){
			System.out.println("Picklist write failed");
			return;

		}
	}
}

class Box {
	private TreeSet<OrderItem> items;
	private Double totalWeight = 0.0;
	private Comparator<OrderItem> byCode;
	Box(){
		byCode = new Comparator<OrderItem>(){
			@Override
			public int compare(OrderItem I1, OrderItem I2) {
				return I1.getCode().compareTo(I2.getCode());
			}		
		};
		items = new TreeSet<OrderItem>(byCode);
	}

	public void add(String code, int quant, Double weight ){
		OrderItem tempItem = new OrderItem(code, quant, weight);
		totalWeight += weight;
		if((items.ceiling(tempItem) != null) && items.ceiling(tempItem).getCode().equals(code)){
			items.ceiling(tempItem).addQuantity(quant); 	
		}else{
			items.add(tempItem);
		}
	}
	public Double getWeight(){
		return(totalWeight);
	}
	public int getCount(){
		return(items.size());
	}
	public String getCode(){
		return(items.first().getCode());
	}
	public int getQuant(){
		if(items.isEmpty()){
			return(0);
		}else{
		return(items.first().getQuantity());
		}
	}
	public void nukeFirst() {
		items.remove(items.first());
	}
}

