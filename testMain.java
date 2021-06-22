package testRepository;

import java.util.Scanner;

public class testMain {

	public static void main(String[] args) {
		
		System.out.println("Please Enter your name: ");
		Scanner scan = new Scanner(System.in);
		String userName = scan.next();
		System.out.println("Thank you for joining us " + userName + "!");
		//This is a comment
	}
}
