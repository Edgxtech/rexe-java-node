import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tech.edgx.dp.usercrud.DP;
import tech.edgx.dp.usercrud.model.User;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DPTest {

    @Test
    @Order(1)
    public void testCreate() {
        String username = "testuser";
        User user = (User) DP.create(username);
        System.out.println("Result: "+new Gson().toJson(user));
        Assert.assertTrue("User created", username.equals(user.getUsername()) && user.getEmail().equals(username+"@test.com"));
    }

    @Test
    @Order(2)
    public void testRetrieve() {
        String username = "testuser";
        User user = DP.retrieve("testuser");
        Assert.assertTrue("User retrieved", username.equals(user.getUsername()) && user.getEmail().equals(username+"@test.com"));
    }

    @Test
    @Order(3)
    public void testUpdate() {
        String result = DP.update("testuser", "test.user.new@test.com");
        System.out.println("Result: "+result);
        Assert.assertTrue("User update ok", result.equals("update: ok"));
    }

    @Test
    @Order(4)
    public void testDelete() {
        String result = DP.delete("testuser");
        System.out.println("Result: "+result);
        Assert.assertTrue("User deletion ok", result.equals("delete: ok"));
    }
}
