// FOR IF STATIC METHODS
//    @Test
//    public void testStart() throws Exception {
//
//        try (MockedStatic<DP> mockDp = Mockito.mockStatic(DP.class)) {
//            mockDp.when(() -> DP.retrieveUser(TEST_USERNAME_A)).thenReturn(
//                    new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//            );
//            mockDp.when(() -> DP.retrieveUser(TEST_USERNAME_B)).thenReturn(
//                    new User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
//            );
//
//            Integer chatId = DP.start(TEST_USERNAME_A, TEST_USERNAME_B);
//            System.out.println("Started chat: "+chatId);
//        }

////
////        Integer chatId = DP.start(TEST_USERNAME_A, TEST_USERNAME_B);
////        System.out.println("Started chat: "+chatId);
//    }