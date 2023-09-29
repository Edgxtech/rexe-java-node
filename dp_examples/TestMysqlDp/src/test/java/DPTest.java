import org.junit.Assert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tech.edgx.dp.mysqlcrud.DP;
import tech.edgx.dp.mysqlcrud.model.User;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DPTest {
    @Test
    @Order(1)
    public void testInsert() {
        String result = DP.insert("testuser");
        System.out.println("Result: "+result);
        Assert.assertTrue("User created", result.equals("insert: ok"));
    }

    @Test
    @Order(2)
    public void testRetrieve() {
        String username="testuser";
        User user = DP.retrieve("testuser");
        System.out.println("Result: "+user.getUsername()+", "+user.getEmail());
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
        Assert.assertTrue("User delete ok", result.equals("delete: ok"));
    }
}
