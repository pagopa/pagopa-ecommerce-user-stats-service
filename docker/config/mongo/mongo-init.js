conn = new Mongo();
db = conn.getDB("ecommerce");

db.getCollection('user-stats').insertMany([
  {
    "_id": "593edea4-bf5b-4dfc-9efc-d5c4359de613",
    "lastUsage": {
      "instrumentId": "b127a1ac-0773-42a0-aa2d-f45dcbc2164f",
      "date": "2023-08-07T15:32:56.592837917Z",
      "type": "WALLET"
    }
  },
  {
    "_id": "16cedd18-0b4d-46c3-b739-d1583db6145e",
    "lastUsage": {
      "instrumentId": "fe2b5e7e-8559-4f05-a778-81f5f8b863d6",
      "date": "2023-08-09T15:32:56.592837917Z",
      "type": "GUEST"
    }
  },
  {
    "_id": "0d6031b3-fdcb-4988-b147-3d68b93a088c",
    "lastUsage": {
      "instrumentId": "02b23789-b1db-4929-b52f-e6dcdbb0928f",
      "date": "2023-08-11T15:32:56.592837917Z",
      "type": "GUEST"
    }
  },
  {
    "_id": "bb5d9fce-3d60-4f94-890b-f8c4c5c6ff64",
    "lastUsage": {
      "instrumentId": "26e0b5b7-9739-486e-a1be-dce2ba62c0f4",
      "date": "2023-08-15T15:32:56.592837917Z",
      "type": "WALLET"
    }
  },
  {
    "_id": "3cc0064d-0d40-4969-80c1-1b3ec50cab76",
    "lastUsage": {
      "instrumentId": "ffe16720-4226-4b89-b504-05ecede007e8",
      "date": "2023-08-20T15:32:56.592837917Z",
      "type": "WALLET"
    }
  },
  {
    "_id": "ccea462e-8649-43c3-bf60-256d2e03ae81",
    "lastUsage": {
      "instrumentId": "3c1cfa37-c8d3-463a-807d-7f25ba3438ce",
      "date": "2023-08-22T15:32:56.592837917Z",
      "type": "GUEST"
    }
  },
  {
    "_id": "8dc2e80a-7a26-4aed-be34-fcc304cc1e90",
    "lastUsage": {
      "instrumentId": "a7e0a97e-c622-4238-ab96-f583c5d90fc7",
      "date": "2023-09-15T15:32:56.592837917Z",
      "type": "WALLET"
    }
  }
]);
