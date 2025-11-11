import React, { useState } from 'react';
import { Layout } from './components/Layout';
import { PetsManager } from './components/PetsManager';
import { CustomersManager } from './components/CustomersManager';
import { SalesManager } from './components/SalesManager';
import { SalesReport } from './components/SalesReport';

function App() {
  const [activeTab, setActiveTab] = useState('pets');

  const renderContent = () => {
    switch (activeTab) {
      case 'pets':
        return <PetsManager />;
      case 'customers':
        return <CustomersManager />;
      case 'sales':
        return <SalesManager />;
      case 'reports':
        return <SalesReport />;
      default:
        return <PetsManager />;
    }
  };

  return (
    <Layout activeTab={activeTab} onTabChange={setActiveTab}>
      {renderContent()}
    </Layout>
  );
}

export default App;
