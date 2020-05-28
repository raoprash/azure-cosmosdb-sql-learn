package edu.common;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.domain.User;

public class JsonReader {
    
    ObjectMapper mapper = new ObjectMapper();

    public User readUser(final String  userFile) throws Exception{
            
        return mapper.readValue(new File(userFile), User.class);
        
   }

   public List<User> readUsers(final String usersFile) throws Exception {
       return mapper.readValue(new File(usersFile), new TypeReference<List<User>>(){});
   }

   public static void main(String[] args) {
        JsonReader reader = new JsonReader();

       try{
       User user = reader.readUser("target/classes/user.json");
       System.out.println(user);
       List<User> users = reader.readUsers("target/classes/users.json");
        System.out.println("Printing Multiple .... \n" + users);
        System.out.println("Printing Multiple using stream iterator.... \n");
        users.stream().forEach(System.out::println);
       }catch(Exception e){
           e.printStackTrace();
       }
   }
}